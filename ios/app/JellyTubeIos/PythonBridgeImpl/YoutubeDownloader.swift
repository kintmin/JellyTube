import Foundation
import os
import shared

private let downloadLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "YoutubeDownload")

/// 전송 자체는 끝났지만 받은 것이 음원이 아닐 때.
enum YoutubeDownloadError: LocalizedError {
    /// 서명이 만료됐거나 헤더가 달라 유튜브가 거부한 경우. 본문이 오류 페이지라 파일이 생긴다.
    case httpStatus(Int)
    /// 200 이지만 본문이 비어 있는 경우.
    case emptyBody
    /// 206 인데 `Content-Range` 가 없어 남은 분량을 셀 수 없는 경우.
    case unknownTotalBytes
    case sizeMismatch(received: Int64, expected: Int64)

    var errorDescription: String? {
        switch self {
        case .httpStatus(let code): return "음원 주소가 거부됐습니다 (HTTP \(code))"
        case .emptyBody: return "받은 음원이 비어 있습니다"
        case .unknownTotalBytes: return "음원 길이를 알 수 없습니다"
        case .sizeMismatch(let received, let expected):
            return "받은 크기가 맞지 않습니다 (\(received)/\(expected))"
        }
    }
}

/// 추출 단계에서 나온 메타데이터. 전송이 끝난 뒤 Kotlin 에 그대로 넘어간다.
struct YoutubeAudioSource: Codable {
    let sourceUrl: String
    let destinationPath: String
    let fileNameWithExt: String
    let title: String
    let thumbnailUrl: String
    let duration: String
    let uploader: String
    let description: String
}

/// 진행 중인 전송. 청크가 끝나는 시점이 앱의 다음 생애일 수 있어 디스크에 적어 둔다.
private struct YoutubeTransfer: Codable {
    let source: YoutubeAudioSource
    let remoteUrl: String
    let headers: [String: String]
    /// 추출이 알려 주거나, 못 받았으면 첫 응답의 `Content-Range` 로 알게 된다.
    var totalBytes: Int64?
    /// 이미 받아 `.part` 에 써 둔 구간의 시작 오프셋.
    var receivedStarts: [Int64]
    /// 남은 구간까지 전부 등록을 마쳤는지.
    var didEnqueueAll: Bool

    /// 완주한 뒤에야 목적지로 옮긴다. 중간에 끊겨도 반쪽 파일이 자리에 남지 않는다.
    var partPath: String { source.destinationPath + ".part" }
}

/// 유튜브 음원을 background `URLSession` 으로 내려받는다. 전송 주체가 OS 데몬이라 앱이
/// 내려가도 이어지고, 사용자가 앱 스위처에서 강제 종료하면 OS 가 전부 취소한다.
///
/// 한 요청이 [RangeChunkPlan.MAX_CHUNK_BYTES] 를 넘는 범위를 달라고 하면 CDN 이 속도를 조이므로
/// 그 상한으로 끊고, **구간 전부를 앱이 떠 있는 동안 등록한다** — 하나씩 이어서 등록하면
/// 그때마다 OS 가 앱을 깨워야 해서 서스펜드 상태에서 청크당 10초를 넘게 기다린다.
final class YoutubeDownloader: NSObject {

    static let shared = YoutubeDownloader()

    private static let sessionIdentifier = "com.kintmin.JellyTubeIos.youtube-download"
    private static let inFlightKey = "youtubeDownload.inFlight"
    private static let completedKey = "youtubeDownload.completed"

    /// 백그라운드 세션을 되살려 달라고 OS 가 앱을 깨울 때 쥐여 주는 마무리 콜백.
    var backgroundCompletionHandler: (() -> Void)?

    private lazy var session: URLSession = {
        let configuration = URLSessionConfiguration.background(withIdentifier: Self.sessionIdentifier)
        // 사용자가 누른 다운로드다. 시스템이 한가할 때까지 미루면 안 된다.
        configuration.isDiscretionary = false
        configuration.sessionSendsLaunchEvents = true
        return URLSession(configuration: configuration, delegate: self, delegateQueue: nil)
    }()

    private let lock = NSLock()
    private var waiters: [String: (Error?) -> Void] = [:]
    private var startedAt: [String: Date] = [:]

    private override init() {
        super.init()
    }

    /// 앱이 뜰 때 한 번 불러 세션을 되살린다. 그래야 지난 생애의 완료 콜백이 도착한다.
    func activate() {
        _ = session
    }

    /// 지난 생애에 끝난 전송의 원본 주소. **기록은 지우지 않는다** —
    /// 이 주소로 다시 요청이 와야 [consumeCompleted] 가 그 결과를 돌려주고 그때 지운다.
    func finishedSourceUrls() -> [String] {
        lock.lock()
        defer { lock.unlock() }

        return Array(Self.load(YoutubeAudioSource.self, key: Self.completedKey).keys)
    }

    /// 같은 주소의 결과가 이미 디스크에 있으면 돌려준다. 전송을 다시 하지 않기 위해서다.
    func consumeCompleted(sourceUrl: String) -> YoutubeAudioSource? {
        lock.lock()
        defer { lock.unlock() }

        var completed = Self.load(YoutubeAudioSource.self, key: Self.completedKey)
        guard let source = completed.removeValue(forKey: sourceUrl) else { return nil }
        Self.save(completed, key: Self.completedKey)

        // 파일이 사라졌으면 기록만 지우고 다시 받게 한다.
        guard FileManager.default.fileExists(atPath: source.destinationPath) else { return nil }
        return source
    }

    func download(
        from remoteUrl: URL,
        headers: [String: String],
        totalBytes: Int64?,
        source: YoutubeAudioSource,
        completion: @escaping (Error?) -> Void
    ) {
        var transfer = YoutubeTransfer(
            source: source,
            remoteUrl: remoteUrl.absoluteString,
            headers: headers,
            totalBytes: totalBytes,
            receivedStarts: [],
            didEnqueueAll: totalBytes != nil
        )
        // 새로 추출한 주소다. 전에 받아 둔 조각이 같은 바이트라는 보장이 없어 버린다.
        try? FileManager.default.removeItem(atPath: transfer.partPath)

        lock.lock()
        waiters[source.sourceUrl] = completion
        startedAt[source.sourceUrl] = Date()
        Self.put(transfer)
        lock.unlock()

        guard let totalBytes else {
            // 크기를 모르면 첫 응답의 `Content-Range` 로 알아내고 그때 나머지를 등록한다.
            request(RangeChunkPlan.shared.first(), for: transfer)
            return
        }

        RangeChunkPlan.shared.chunks(totalBytes: totalBytes).forEach { request($0, for: transfer) }
    }

    private func request(_ chunk: RangeChunk, for transfer: YoutubeTransfer) {
        guard let url = URL(string: transfer.remoteUrl) else {
            fail(sourceUrl: transfer.source.sourceUrl, error: URLError(.badURL))
            return
        }

        var request = URLRequest(url: url)
        // 유튜브가 주는 주소는 서명된 단기 주소다. 추출 때와 다른 헤더로 요청하면 거부될 수 있어
        // yt-dlp 가 준 헤더를 그대로 싣는다.
        for (field, value) in transfer.headers {
            request.setValue(value, forHTTPHeaderField: field)
        }
        // 압축이 끼면 `Content-Range` 의 바이트 수와 받은 바이트 수가 어긋난다.
        request.setValue("identity", forHTTPHeaderField: "Accept-Encoding")
        request.setValue(chunk.headerValue, forHTTPHeaderField: "Range")

        let task = session.downloadTask(with: request)
        // `taskIdentifier` 는 세션 인스턴스 안에서만 유일하다. 앱이 다시 뜨면 1 부터 다시 매겨져
        // 지난 생애의 기록과 엉킨다. 기록은 주소로 찾고, 태스크에는 그 주소를 붙여 둔다.
        task.taskDescription = transfer.source.sourceUrl

        downloadLog.info("start task=\(task.taskIdentifier) range=\(chunk.headerValue, privacy: .public)")
        task.resume()
    }

    // MARK: - 기록

    private static func put(_ transfer: YoutubeTransfer) {
        var inFlight = load(YoutubeTransfer.self, key: inFlightKey)
        inFlight[transfer.source.sourceUrl] = transfer
        save(inFlight, key: inFlightKey)
    }

    private static func load<T: Decodable>(_ type: T.Type, key: String) -> [String: T] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([String: T].self, from: data)
        else { return [:] }
        return decoded
    }

    private static func save<T: Encodable>(_ value: [String: T], key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

extension YoutubeDownloader: URLSessionDownloadDelegate {

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        guard let sourceUrl = downloadTask.taskDescription else { return }

        lock.lock()
        let stored = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)[sourceUrl]
        lock.unlock()

        guard var transfer = stored else {
            downloadLog.error("no record for task=\(downloadTask.taskIdentifier)")
            return
        }

        // `URLSessionDownloadTask` 는 403 도 "전송 성공"으로 보고 오류 본문을 파일로 내려 준다.
        // 여기서 걸러내지 않으면 내용 없는 m4a 가 DB 에 등록된다.
        let response = downloadTask.response as? HTTPURLResponse
        let statusCode = response?.statusCode ?? 0
        let byteCount = Self.fileSize(atPath: location.path)

        guard statusCode == 200 || statusCode == 206 else {
            fail(sourceUrl: sourceUrl, error: YoutubeDownloadError.httpStatus(statusCode))
            return
        }
        guard byteCount > 0 else {
            fail(sourceUrl: sourceUrl, error: YoutubeDownloadError.emptyBody)
            return
        }

        let reportedTotal = RangeChunkPlan.shared.parseTotalBytes(
            contentRange: response?.value(forHTTPHeaderField: "Content-Range")
        )?.int64Value

        // 200 은 서버가 `Range` 를 무시하고 전체를 보낸 것이다. 조각으로 섞으면 깨진다.
        let isWholeBody = statusCode == 200
        if isWholeBody {
            try? FileManager.default.removeItem(atPath: transfer.partPath)
            transfer.receivedStarts = []
            transfer.totalBytes = byteCount
            transfer.didEnqueueAll = true
        } else if let totalBytes = transfer.totalBytes {
            // 추출이 알려 준 크기와 서버가 말하는 크기가 다르면 구간 계획 전체가 틀린 것이다.
            guard reportedTotal == nil || reportedTotal == totalBytes else {
                fail(
                    sourceUrl: sourceUrl,
                    error: YoutubeDownloadError.sizeMismatch(
                        received: reportedTotal ?? 0,
                        expected: totalBytes
                    )
                )
                return
            }
        } else {
            // 첫 요청은 늘 상한만큼 달라고 하므로, 그보다 적게 왔다면 서버가 파일 끝에 닿은 것이다.
            let reachedEnd = byteCount < RangeChunkPlan.shared.MAX_CHUNK_BYTES
            transfer.totalBytes = reportedTotal ?? (reachedEnd ? byteCount : nil)
        }

        guard let totalBytes = transfer.totalBytes else {
            fail(sourceUrl: sourceUrl, error: YoutubeDownloadError.unknownTotalBytes)
            return
        }

        let offset = isWholeBody
            ? 0
            : RangeChunkPlan.shared.parseRequestedStart(
                rangeHeader: downloadTask.originalRequest?.value(forHTTPHeaderField: "Range")
            )?.int64Value ?? 0

        // 이 함수가 끝나면 OS 가 임시 파일을 지운다. 반드시 여기서 옮겨야 한다.
        do {
            try Self.write(
                contentsOf: location,
                to: transfer.partPath,
                at: offset,
                totalBytes: totalBytes
            )
        } catch {
            downloadLog.error("write failed: \(error.localizedDescription, privacy: .public)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }

        if !transfer.receivedStarts.contains(offset) {
            transfer.receivedStarts.append(offset)
        }

        let missing = RangeChunkPlan.shared.chunks(totalBytes: totalBytes)
            .filter { !transfer.receivedStarts.contains($0.start) }

        let shouldEnqueueRest = !transfer.didEnqueueAll
        transfer.didEnqueueAll = true

        lock.lock()
        Self.put(transfer)
        lock.unlock()

        if shouldEnqueueRest {
            missing.forEach { request($0, for: transfer) }
            return
        }
        if missing.isEmpty {
            finalize(transfer, totalBytes: totalBytes)
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let error, let sourceUrl = task.taskDescription else { return }
        // 취소는 우리가 낸 것이다 — 한 구간이 깨지면 같은 주소의 나머지를 접는다.
        guard (error as? URLError)?.code != .cancelled else { return }

        fail(sourceUrl: sourceUrl, error: error)
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        // 구간이 더 남아 있으면 아직 끝난 것이 아니다.
        lock.lock()
        let hasPending = !Self.load(YoutubeTransfer.self, key: Self.inFlightKey).isEmpty
        lock.unlock()
        guard !hasPending else { return }

        DispatchQueue.main.async {
            self.backgroundCompletionHandler?()
            self.backgroundCompletionHandler = nil
        }
    }

    private func finalize(_ transfer: YoutubeTransfer, totalBytes: Int64) {
        let sourceUrl = transfer.source.sourceUrl

        lock.lock()
        let elapsed = startedAt[sourceUrl].map { Date().timeIntervalSince($0) } ?? 0
        lock.unlock()

        let partBytes = Self.fileSize(atPath: transfer.partPath)
        downloadLog.info("finish bytes=\(partBytes) elapsed=\(elapsed)s")

        guard partBytes == totalBytes else {
            fail(
                sourceUrl: sourceUrl,
                error: YoutubeDownloadError.sizeMismatch(received: partBytes, expected: totalBytes)
            )
            return
        }

        let destination = URL(fileURLWithPath: transfer.source.destinationPath)
        do {
            try FileManager.default.createDirectory(
                at: destination.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }
            try FileManager.default.moveItem(atPath: transfer.partPath, toPath: destination.path)
        } catch {
            downloadLog.error("move failed: \(error.localizedDescription, privacy: .public)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }

        finish(sourceUrl: sourceUrl, error: nil, source: transfer.source)
    }

    private func fail(sourceUrl: String, error: Error) {
        lock.lock()
        let transfer = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)[sourceUrl]
        lock.unlock()

        downloadLog.error("failed: \(error.localizedDescription, privacy: .public)")

        // 같은 주소의 나머지 구간은 이미 등록돼 돌고 있다. 접지 않으면 그 응답이
        // 재추출로 새로 시작한 전송의 기록에 끼어든다.
        session.getAllTasks { tasks in
            tasks
                .filter { $0.taskDescription == sourceUrl }
                .forEach { $0.cancel() }

            if let transfer {
                try? FileManager.default.removeItem(atPath: transfer.partPath)
            }
            self.finish(sourceUrl: sourceUrl, error: error, source: nil)
        }
    }

    /// 기다리던 쪽이 있으면 바로 알리고, 없으면(앱이 새로 뜬 경우) 결과를 적어 둔다.
    private func finish(sourceUrl: String, error: Error?, source: YoutubeAudioSource?) {
        lock.lock()
        var inFlight = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)
        inFlight.removeValue(forKey: sourceUrl)
        Self.save(inFlight, key: Self.inFlightKey)
        startedAt.removeValue(forKey: sourceUrl)
        let waiter = waiters.removeValue(forKey: sourceUrl)
        if waiter == nil, let source {
            var completed = Self.load(YoutubeAudioSource.self, key: Self.completedKey)
            completed[sourceUrl] = source
            Self.save(completed, key: Self.completedKey)
        }
        lock.unlock()

        waiter?(error)
    }

    private static func fileSize(atPath path: String) -> Int64 {
        let attributes = try? FileManager.default.attributesOfItem(atPath: path)
        return (attributes?[.size] as? NSNumber)?.int64Value ?? 0
    }

    /// 구간을 제 오프셋에 쓴다. 미리 전체 길이를 잡아 두어 조각이 어떤 순서로 와도 된다.
    private static func write(
        contentsOf location: URL,
        to path: String,
        at offset: Int64,
        totalBytes: Int64
    ) throws {
        let manager = FileManager.default
        try manager.createDirectory(
            at: URL(fileURLWithPath: path).deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        if !manager.fileExists(atPath: path) {
            manager.createFile(atPath: path, contents: nil)
        }

        let handle = try FileHandle(forWritingTo: URL(fileURLWithPath: path))
        defer { try? handle.close() }
        try handle.truncate(atOffset: UInt64(totalBytes))
        try handle.seek(toOffset: UInt64(offset))
        try handle.write(contentsOf: Data(contentsOf: location, options: .mappedIfSafe))
    }
}
