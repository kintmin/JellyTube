import Foundation
import shared
import UIKit

/// 공유 앱 로그로 보낸다. `os_log` 로만 남기면 `app_logs` 에 안 가서 기기에서 뽑아 볼 수 없다.
private func downloadLog(_ message: String) {
    SwiftAppLogKt.swiftDebugLog(tag: "YoutubeDownload", message: message)
}

private func seconds(since start: Date?) -> String {
    guard let start else { return "?" }
    return String(format: "%.2f", Date().timeIntervalSince(start))
}

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
    /// 실행 유예가 끝나 OS 데몬에 넘긴 전송인지. 앱 생애를 넘겨도 같은 세션으로 이어야 한다.
    var usesBackgroundSession: Bool
    /// HLS 조각 주소 목록(JSON 배열) 파일. 있으면 범위 요청 대신 조각을 받는다.
    /// 목록이 길어 전송 기록(UserDefaults)에 넣지 않는다 — 조각마다 기록을 통째로 다시 쓴다.
    var segmentsPath: String?
    /// 받아 둔 조각 번호.
    var receivedSegments: [Int]?

    /// 완주한 뒤에야 목적지로 옮긴다. 중간에 끊겨도 반쪽 파일이 자리에 남지 않는다.
    var partPath: String { source.destinationPath + ".part" }
    /// 조각은 크기를 미리 몰라 오프셋에 쓸 수 없다. 번호별 파일로 두었다가 다 모이면 이어 붙인다.
    var segmentDirectory: String { partPath + ".seg" }
}

/// 유튜브 음원을 세션 두 벌로 내려받는다.
///
/// 한 요청이 [RangeChunkPlan.MAX_CHUNK_BYTES] 를 넘는 범위를 달라고 하면 CDN 이 속도를 조이므로
/// 그 상한으로 끊고, 구간 전부를 처음에 등록한다.
///
/// 전송은 **기본 세션**으로 시작하고 `beginBackgroundTask` 로 앱을 깨어 있게 한다. 홈으로
/// 내려가도 그 유예 동안은 전속으로 받는다. 서스펜드된 앱의 background 세션 전송은 시스템이
/// 전원·네트워크 상태를 보고 자기 속도로 돌려주기 때문이다 — 같은 48.8MB 를 포그라운드 6.4초,
/// 백그라운드 2~3분으로 실측했다.
///
/// 유예가 만료되면 남은 구간을 **background 세션**으로 넘긴다. 전송 주체가 OS 데몬으로 바뀌어
/// 앱이 서스펜드되거나 죽어도 이어진다. 느리지만 완주는 보장된다. 사용자가 앱 스위처에서
/// 강제 종료하면 그때는 OS 가 전부 취소한다.
final class YoutubeDownloader: NSObject {

    static let shared = YoutubeDownloader()

    private static let sessionIdentifier = "com.kintmin.JellyTubeIos.youtube-download"
    private static let inFlightKey = "youtubeDownload.inFlight"
    private static let completedKey = "youtubeDownload.completed"
    private static let executionWindowName = "youtube-download"
    /// 재시도 간격(초). 개수가 재시도 횟수 상한이다.
    private static let retryDelays: [TimeInterval] = [1, 3]

    /// 백그라운드 세션을 되살려 달라고 OS 가 앱을 깨울 때 쥐여 주는 마무리 콜백.
    var backgroundCompletionHandler: (() -> Void)?

    /// 세션이 둘이라 델리게이트 큐를 공유해야 한다. 그러지 않으면 두 세션의 콜백이 겹쳐
    /// 같은 `.part` 와 같은 기록을 동시에 건드린다.
    private let delegateQueue: OperationQueue = {
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 1
        return queue
    }()

    private lazy var foregroundSession = URLSession(
        configuration: .default,
        delegate: self,
        delegateQueue: delegateQueue
    )

    private lazy var backgroundSession: URLSession = {
        let configuration = URLSessionConfiguration.background(withIdentifier: Self.sessionIdentifier)
        // 사용자가 누른 다운로드다. 시스템이 한가할 때까지 미루면 안 된다.
        configuration.isDiscretionary = false
        configuration.sessionSendsLaunchEvents = true
        return URLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)
    }()

    private let lock = NSLock()
    private var waiters: [String: (Error?) -> Void] = [:]
    private var startedAt: [String: Date] = [:]
    /// `주소#시작오프셋` · `주소#seg번호` 별 재시도 횟수.
    private var chunkRetryCounts: [String: Int] = [:]
    /// `segmentsPath` 별 조각 주소. 응답마다 파일을 다시 읽지 않으려고 둔다.
    private var segmentUrlsCache: [String: [String]] = [:]
    /// 메인 스레드에서만 만진다.
    private var executionWindow: UIBackgroundTaskIdentifier = .invalid
    /// 전송 기록이 생기기 전 단계(추출)가 몇 건 돌고 있는지.
    private var pendingExtractionCount = 0

    private override init() {
        super.init()
    }

    /// 앱이 뜰 때 한 번 불러 세션을 되살린다. 그래야 지난 생애의 완료 콜백이 도착한다.
    func activate() {
        _ = backgroundSession
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
            didEnqueueAll: totalBytes != nil,
            usesBackgroundSession: false
        )
        // 새로 추출한 주소다. 전에 받아 둔 조각이 같은 바이트라는 보장이 없어 버린다.
        try? FileManager.default.removeItem(atPath: transfer.partPath)

        lock.lock()
        waiters[source.sourceUrl] = completion
        startedAt[source.sourceUrl] = Date()
        Self.put(transfer)
        lock.unlock()
        refreshExecutionWindow()

        guard let totalBytes else {
            // 크기를 모르면 첫 응답의 `Content-Range` 로 알아내고 그때 나머지를 등록한다.
            downloadLog("begin totalBytes=unknown")
            request(RangeChunkPlan.shared.first(), for: transfer)
            return
        }

        let chunks = RangeChunkPlan.shared.chunks(totalBytes: totalBytes)
        downloadLog("begin totalBytes=\(totalBytes) chunks=\(chunks.count)")
        chunks.forEach { request($0, for: transfer) }
    }

    /// HLS 조각을 전부 한 번에 등록하고, 다 모이면 순서대로 이어 붙인다.
    func downloadSegments(
        playlistUrl: URL,
        segmentsPath: String,
        headers: [String: String],
        source: YoutubeAudioSource,
        completion: @escaping (Error?) -> Void
    ) {
        let transfer = YoutubeTransfer(
            source: source,
            remoteUrl: playlistUrl.absoluteString,
            headers: headers,
            totalBytes: nil,
            receivedStarts: [],
            didEnqueueAll: true,
            usesBackgroundSession: false,
            segmentsPath: segmentsPath,
            receivedSegments: []
        )
        try? FileManager.default.removeItem(atPath: transfer.partPath)
        try? FileManager.default.removeItem(atPath: transfer.segmentDirectory)

        lock.lock()
        waiters[source.sourceUrl] = completion
        startedAt[source.sourceUrl] = Date()
        Self.put(transfer)
        lock.unlock()
        refreshExecutionWindow()

        guard let urls = segmentUrls(for: transfer), !urls.isEmpty else {
            fail(sourceUrl: source.sourceUrl, error: CocoaError(.fileReadCorruptFile))
            return
        }

        downloadLog("begin segments=\(urls.count)")
        for (index, url) in urls.enumerated() {
            requestSegment(index, url: url, for: transfer)
        }
    }

    private func request(_ chunk: RangeChunk, for transfer: YoutubeTransfer, delay: TimeInterval = 0) {
        guard let url = URL(string: transfer.remoteUrl) else {
            fail(sourceUrl: transfer.source.sourceUrl, error: URLError(.badURL))
            return
        }

        var request = makeRequest(url, for: transfer)
        request.setValue(chunk.headerValue, forHTTPHeaderField: "Range")
        enqueue(request, for: transfer, label: "range=\(chunk.headerValue)", delay: delay)
    }

    private func requestSegment(_ index: Int, url: String, for transfer: YoutubeTransfer, delay: TimeInterval = 0) {
        guard let url = URL(string: url) else {
            fail(sourceUrl: transfer.source.sourceUrl, error: URLError(.badURL))
            return
        }
        enqueue(makeRequest(url, for: transfer), for: transfer, label: "segment=\(index)", delay: delay)
    }

    private func makeRequest(_ url: URL, for transfer: YoutubeTransfer) -> URLRequest {
        var request = URLRequest(url: url)
        // 유튜브가 주는 주소는 서명된 단기 주소다. 추출 때와 다른 헤더로 요청하면 거부될 수 있어
        // yt-dlp 가 준 헤더를 그대로 싣는다.
        for (field, value) in transfer.headers {
            request.setValue(value, forHTTPHeaderField: field)
        }
        // 압축이 끼면 `Content-Range` 의 바이트 수와 받은 바이트 수가 어긋난다.
        request.setValue("identity", forHTTPHeaderField: "Accept-Encoding")
        return request
    }

    private func enqueue(_ request: URLRequest, for transfer: YoutubeTransfer, label: String, delay: TimeInterval) {
        let session = transfer.usesBackgroundSession ? backgroundSession : foregroundSession
        let task = session.downloadTask(with: request)
        // `taskIdentifier` 는 세션 인스턴스 안에서만 유일하다. 앱이 다시 뜨면 1 부터 다시 매겨져
        // 지난 생애의 기록과 엉킨다. 기록은 주소로 찾고, 태스크에는 그 주소를 붙여 둔다.
        task.taskDescription = transfer.source.sourceUrl

        // 조각은 수백 개라 처음 등록까지 찍으면 로그가 넘친다. 재시도만 남긴다.
        if transfer.segmentsPath == nil || delay > 0 {
            downloadLog(
                "request \(label) delay=\(delay)s "
                    + "session=\(transfer.usesBackgroundSession ? "background" : "foreground")"
            )
        }
        guard delay > 0 else {
            task.resume()
            return
        }
        if transfer.usesBackgroundSession {
            // 서스펜드된 앱은 타이머를 못 돌린다. 기다리는 것도 OS 데몬에 맡긴다.
            task.earliestBeginDate = Date().addingTimeInterval(delay)
            task.resume()
        } else {
            // 기다리는 동안 취소되면 `resume` 은 아무 일도 하지 않는다.
            DispatchQueue.global().asyncAfter(deadline: .now() + delay) { task.resume() }
        }
    }

    // MARK: - 실행 유예

    /// 추출도 앱 프로세스에서 돈다. 유예가 없으면 홈으로 내려가는 순간 멈춰서 받을 구간
    /// 목록조차 만들지 못하고, 앱으로 돌아올 때까지 아무 일도 일어나지 않는다.
    func beginExtraction() {
        lock.lock()
        pendingExtractionCount += 1
        lock.unlock()
        refreshExecutionWindow()
    }

    func endExtraction() {
        lock.lock()
        pendingExtractionCount = max(0, pendingExtractionCount - 1)
        lock.unlock()
        refreshExecutionWindow()
    }

    /// 할 일이 남아 있으면 앱을 깨어 있게 하고, 없으면 유예를 반납한다.
    private func refreshExecutionWindow() {
        DispatchQueue.main.async {
            // 메인에 들어온 뒤에 읽어야 한다. 밖에서 재면 "A 끝남(false) → B 시작(true)" 이
            // 메인에서 뒤집혀 들어와 진행 중인 B 가 유예 없이 남는다.
            self.lock.lock()
            let hasPending = !Self.load(YoutubeTransfer.self, key: Self.inFlightKey).isEmpty
                || self.pendingExtractionCount > 0
            self.lock.unlock()

            if hasPending, self.executionWindow == .invalid {
                self.executionWindow = UIApplication.shared.beginBackgroundTask(
                    withName: Self.executionWindowName
                ) {
                    self.handOffToBackgroundSession()
                }
            } else if !hasPending, self.executionWindow != .invalid {
                UIApplication.shared.endBackgroundTask(self.executionWindow)
                self.executionWindow = .invalid
            }
        }
    }

    /// 유예가 만료될 때 남은 구간을 OS 데몬에 넘긴다. 넘기지 않으면 서스펜드와 함께 전송이 죽는다.
    ///
    /// **등록을 먼저 하고 취소를 뒤에 한다** — 반대로 하면 취소와 등록 사이에 서스펜드되어
    /// 아무 전송도 남지 않을 수 있다. 같은 구간이 두 세션에서 겹쳐 와도 바이트가 같고
    /// 제 오프셋에 쓰므로 해가 없다.
    private func handOffToBackgroundSession() {
        downloadLog("window expired — background 세션으로 넘긴다")
        lock.lock()
        var inFlight = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)
        for key in inFlight.keys {
            inFlight[key]?.usesBackgroundSession = true
        }
        Self.save(inFlight, key: Self.inFlightKey)
        lock.unlock()

        for transfer in inFlight.values {
            if transfer.segmentsPath != nil {
                let received = Set(transfer.receivedSegments ?? [])
                for (index, url) in (segmentUrls(for: transfer) ?? []).enumerated() where !received.contains(index) {
                    requestSegment(index, url: url, for: transfer)
                }
                continue
            }
            guard let totalBytes = transfer.totalBytes else {
                request(RangeChunkPlan.shared.first(), for: transfer)
                continue
            }
            RangeChunkPlan.shared.chunks(totalBytes: totalBytes)
                .filter { !transfer.receivedStarts.contains($0.start) }
                .forEach { request($0, for: transfer) }
        }

        if executionWindow != .invalid {
            UIApplication.shared.endBackgroundTask(executionWindow)
            executionWindow = .invalid
        }

        // 서스펜드되면 기본 세션의 태스크는 어차피 죽는다. 포그라운드로 바로 돌아오는 경우만
        // 겹치므로 접어 두되, 유예 반납을 여기에 매달지 않는다.
        foregroundSession.getAllTasks { $0.forEach { $0.cancel() } }
    }

    // MARK: - 기록

    private func segmentUrls(for transfer: YoutubeTransfer) -> [String]? {
        guard let path = transfer.segmentsPath else { return nil }

        lock.lock()
        defer { lock.unlock() }
        if let cached = segmentUrlsCache[path] { return cached }
        guard let data = FileManager.default.contents(atPath: path),
              let urls = try? JSONDecoder().decode([String].self, from: data)
        else { return nil }
        segmentUrlsCache[path] = urls
        return urls
    }

    /// 응답이 몇 번 조각인지. 유튜브 조각 주소는 `/sq/번호` 가 있어 서로 다르다.
    private func segmentIndex(of task: URLSessionTask, in transfer: YoutubeTransfer) -> Int? {
        guard let url = task.originalRequest?.url?.absoluteString else { return nil }
        return segmentUrls(for: transfer)?.firstIndex(of: url)
    }

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
            downloadLog("no record for \(sourceUrl)")
            return
        }

        // `URLSessionDownloadTask` 는 403 도 "전송 성공"으로 보고 오류 본문을 파일로 내려 준다.
        // 여기서 걸러내지 않으면 내용 없는 m4a 가 DB 에 등록된다.
        let response = downloadTask.response as? HTTPURLResponse
        let statusCode = response?.statusCode ?? 0
        let byteCount = Self.fileSize(atPath: location.path)

        if transfer.segmentsPath != nil {
            finishSegment(downloadTask, location: location, transfer: transfer, statusCode: statusCode, byteCount: byteCount)
            return
        }

        guard statusCode == 200 || statusCode == 206 else {
            failOrRetry(downloadTask, sourceUrl: sourceUrl, statusCode: statusCode)
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
            transfer.totalBytes = byteCount
            transfer.didEnqueueAll = true
            // 한 번에 전부 받았으니 계획한 구간이 모두 채워진 것이다. 받은 구간을 `[0]` 으로만
            // 두면 상한을 넘는 파일에서 완주 판정이 서지 않아 전송이 멈춘다.
            transfer.receivedStarts = RangeChunkPlan.shared.chunks(totalBytes: byteCount)
                .map { $0.start }
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

        // 요청한 폭만큼 오지 않으면 그 구간에 구멍이 남는다. `.part` 는 전체 길이로 미리 잡아
        // 두므로 다 받은 뒤 파일 크기를 재도 드러나지 않는다.
        let expectedBytes = min(offset + RangeChunkPlan.shared.MAX_CHUNK_BYTES, totalBytes) - offset
        guard isWholeBody || byteCount == expectedBytes else {
            fail(
                sourceUrl: sourceUrl,
                error: YoutubeDownloadError.sizeMismatch(received: byteCount, expected: expectedBytes)
            )
            return
        }

        // 이 함수가 끝나면 OS 가 임시 파일을 지운다. 반드시 여기서 옮겨야 한다.
        do {
            try Self.write(
                contentsOf: location,
                to: transfer.partPath,
                at: offset,
                totalBytes: totalBytes
            )
        } catch {
            downloadLog("write failed: \(error.localizedDescription)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }

        if !transfer.receivedStarts.contains(offset) {
            transfer.receivedStarts.append(offset)
        }

        lock.lock()
        let startedDate = startedAt[sourceUrl]
        lock.unlock()
        downloadLog(
            "chunk start=\(offset) status=\(statusCode) bytes=\(byteCount) "
                + "received=\(transfer.receivedStarts.count) elapsed=\(seconds(since: startedDate))s"
        )

        let missing = RangeChunkPlan.shared.chunks(totalBytes: totalBytes)
            .filter { !transfer.receivedStarts.contains($0.start) }

        let shouldEnqueueRest = !transfer.didEnqueueAll
        transfer.didEnqueueAll = true

        lock.lock()
        Self.put(transfer)
        lock.unlock()

        guard !missing.isEmpty else {
            finalize(transfer, totalBytes: totalBytes)
            return
        }
        if shouldEnqueueRest {
            missing.forEach { request($0, for: transfer) }
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let error, let sourceUrl = task.taskDescription else { return }
        // 취소는 우리가 낸 것이다 — 한 구간이 깨지면 같은 주소의 나머지를 접는다.
        guard (error as? URLError)?.code != .cancelled else { return }

        // 구간을 전부 동시에 등록하므로 하나가 넘어졌다고 전체를 버리면 실패가 구간 수만큼
        // 잦아진다. 받아 둔 조각은 남아 있으니 그 구간만 다시 받는다.
        if retry(task, sourceUrl: sourceUrl, reason: error.localizedDescription) {
            return
        }

        fail(sourceUrl: sourceUrl, error: error)
    }

    /// 5xx·429 는 서버가 잠시 못 받는 것이라 같은 주소로 다시 받는다. 403·404·410 은 주소가 상한 것이라
    /// 호출자가 다시 추출한다.
    private func failOrRetry(_ task: URLSessionTask, sourceUrl: String, statusCode: Int) {
        let isTransient = statusCode == 429 || (500...599).contains(statusCode)
        if isTransient, retry(task, sourceUrl: sourceUrl, reason: "HTTP \(statusCode)") {
            return
        }
        fail(sourceUrl: sourceUrl, error: YoutubeDownloadError.httpStatus(statusCode))
    }

    /// 같은 구간·조각을 간격을 두고 다시 등록한다. 한도를 넘었거나 어느 구간인지 되읽을 수 없으면 false.
    private func retry(_ task: URLSessionTask, sourceUrl: String, reason: String) -> Bool {
        lock.lock()
        let transfer = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)[sourceUrl]
        lock.unlock()
        guard let transfer else { return false }

        let key: String
        let resend: (TimeInterval) -> Void
        if transfer.segmentsPath != nil {
            guard let index = segmentIndex(of: task, in: transfer),
                  !(transfer.receivedSegments ?? []).contains(index),
                  let url = segmentUrls(for: transfer)?[index]
            else { return false }
            key = "\(sourceUrl)#seg\(index)"
            resend = { self.requestSegment(index, url: url, for: transfer, delay: $0) }
        } else {
            guard let start = RangeChunkPlan.shared.parseRequestedStart(
                      rangeHeader: task.originalRequest?.value(forHTTPHeaderField: "Range")
                  )?.int64Value,
                  !transfer.receivedStarts.contains(start)
            else { return false }
            // 크기를 모르면 첫 구간밖에 등록되지 않았다. 그 첫 구간을 그대로 다시 달라고 한다.
            let chunk = transfer.totalBytes.map {
                RangeChunkPlan.shared.next(receivedBytes: start, totalBytes: $0)
            } ?? (start == 0 ? RangeChunkPlan.shared.first() : nil)
            guard let chunk else { return false }
            key = "\(sourceUrl)#\(start)"
            resend = { self.request(chunk, for: transfer, delay: $0) }
        }

        lock.lock()
        let attempt = (chunkRetryCounts[key] ?? 0) + 1
        chunkRetryCounts[key] = attempt
        lock.unlock()
        guard attempt <= Self.retryDelays.count else { return false }

        downloadLog("retry \(key.dropFirst(sourceUrl.count)) attempt=\(attempt): \(reason)")
        resend(Self.retryDelays[attempt - 1])
        return true
    }

    private func finishSegment(
        _ task: URLSessionDownloadTask,
        location: URL,
        transfer: YoutubeTransfer,
        statusCode: Int,
        byteCount: Int64
    ) {
        let sourceUrl = transfer.source.sourceUrl
        guard statusCode == 200 else {
            failOrRetry(task, sourceUrl: sourceUrl, statusCode: statusCode)
            return
        }
        guard byteCount > 0 else {
            fail(sourceUrl: sourceUrl, error: YoutubeDownloadError.emptyBody)
            return
        }
        guard let index = segmentIndex(of: task, in: transfer),
              let segmentCount = segmentUrls(for: transfer)?.count
        else {
            downloadLog("unknown segment for \(sourceUrl)")
            return
        }

        // 이 함수가 끝나면 OS 가 임시 파일을 지운다. 반드시 여기서 옮겨야 한다.
        do {
            try FileManager.default.createDirectory(
                atPath: transfer.segmentDirectory,
                withIntermediateDirectories: true
            )
            let segmentPath = "\(transfer.segmentDirectory)/\(index)"
            if FileManager.default.fileExists(atPath: segmentPath) {
                try FileManager.default.removeItem(atPath: segmentPath)
            }
            try FileManager.default.moveItem(atPath: location.path, toPath: segmentPath)
        } catch {
            downloadLog("segment move failed: \(error.localizedDescription)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }

        var updated = transfer
        var received = updated.receivedSegments ?? []
        if !received.contains(index) {
            received.append(index)
        }
        updated.receivedSegments = received

        lock.lock()
        Self.put(updated)
        let startedDate = startedAt[sourceUrl]
        lock.unlock()
        if received.count % 50 == 0 {
            downloadLog("segments received=\(received.count)/\(segmentCount) elapsed=\(seconds(since: startedDate))s")
        }

        guard received.count == segmentCount else { return }

        do {
            try Self.concatenateSegments(count: segmentCount, from: updated.segmentDirectory, to: updated.partPath)
        } catch {
            downloadLog("concatenate failed: \(error.localizedDescription)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }
        downloadLog(
            "done bytes=\(Self.fileSize(atPath: updated.partPath)) segments=\(segmentCount) "
                + "elapsed=\(seconds(since: startedDate))s"
        )
        moveToDestination(updated)
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        // 이 세션의 이벤트가 전부 전달된 뒤에만 불린다. 여기서 핸들러를 돌려주지 않으면
        // OS 가 깨워 준 앱을 그대로 강제 종료한다. 구간이 남아 있으면 다음 전달 때 다시 불린다.
        DispatchQueue.main.async {
            self.backgroundCompletionHandler?()
            self.backgroundCompletionHandler = nil
        }
    }

    private func finalize(_ transfer: YoutubeTransfer, totalBytes: Int64) {
        let sourceUrl = transfer.source.sourceUrl

        lock.lock()
        let startedDate = startedAt[sourceUrl]
        lock.unlock()

        let partBytes = Self.fileSize(atPath: transfer.partPath)
        downloadLog(
            "done bytes=\(partBytes) chunks=\(transfer.receivedStarts.count) "
                + "elapsed=\(seconds(since: startedDate))s"
        )

        // `.part` 길이는 미리 잡아 둔 전체 길이라 그것만으로는 다 받았는지 알 수 없다.
        let chunkCount = RangeChunkPlan.shared.chunks(totalBytes: totalBytes).count
        guard transfer.receivedStarts.count == chunkCount, partBytes == totalBytes else {
            fail(
                sourceUrl: sourceUrl,
                error: YoutubeDownloadError.sizeMismatch(received: partBytes, expected: totalBytes)
            )
            return
        }

        moveToDestination(transfer)
    }

    private func moveToDestination(_ transfer: YoutubeTransfer) {
        let sourceUrl = transfer.source.sourceUrl
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
            downloadLog("move failed: \(error.localizedDescription)")
            fail(sourceUrl: sourceUrl, error: error)
            return
        }

        Self.removeSegmentFiles(of: transfer)
        finish(sourceUrl: sourceUrl, error: nil, source: transfer.source)
    }

    private func fail(sourceUrl: String, error: Error) {
        lock.lock()
        let transfer = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)[sourceUrl]
        lock.unlock()

        downloadLog("failed: \(error.localizedDescription)")

        // 같은 주소의 나머지 구간은 이미 등록돼 돌고 있다. 접지 않으면 그 응답이
        // 재추출로 새로 시작한 전송의 기록에 끼어든다.
        cancelTasks(for: sourceUrl) {
            if let transfer {
                try? FileManager.default.removeItem(atPath: transfer.partPath)
                Self.removeSegmentFiles(of: transfer)
            }
            self.finish(sourceUrl: sourceUrl, error: error, source: nil)
        }
    }

    private func cancelTasks(for sourceUrl: String, completion: @escaping () -> Void) {
        let group = DispatchGroup()
        for session in [foregroundSession, backgroundSession] {
            group.enter()
            session.getAllTasks { tasks in
                tasks
                    .filter { $0.taskDescription == sourceUrl }
                    .forEach { $0.cancel() }
                group.leave()
            }
        }
        group.notify(queue: .global(), execute: completion)
    }

    /// 기다리던 쪽이 있으면 바로 알리고, 없으면(앱이 새로 뜬 경우) 결과를 적어 둔다.
    private func finish(sourceUrl: String, error: Error?, source: YoutubeAudioSource?) {
        lock.lock()
        var inFlight = Self.load(YoutubeTransfer.self, key: Self.inFlightKey)
        if let segmentsPath = inFlight.removeValue(forKey: sourceUrl)?.segmentsPath {
            segmentUrlsCache.removeValue(forKey: segmentsPath)
        }
        Self.save(inFlight, key: Self.inFlightKey)
        startedAt.removeValue(forKey: sourceUrl)
        chunkRetryCounts = chunkRetryCounts.filter { !$0.key.hasPrefix("\(sourceUrl)#") }
        let waiter = waiters.removeValue(forKey: sourceUrl)
        if waiter == nil, let source {
            var completed = Self.load(YoutubeAudioSource.self, key: Self.completedKey)
            completed[sourceUrl] = source
            Self.save(completed, key: Self.completedKey)
        }
        lock.unlock()
        refreshExecutionWindow()

        waiter?(error)
    }

    /// 조각을 번호 순으로 이어 붙인다. yt-dlp 의 HLS 다운로더와 같은 결과 바이트다.
    private static func concatenateSegments(count: Int, from directory: String, to path: String) throws {
        let manager = FileManager.default
        if manager.fileExists(atPath: path) {
            try manager.removeItem(atPath: path)
        }
        manager.createFile(atPath: path, contents: nil, attributes: [.posixPermissions: 0o600])

        let handle = try FileHandle(forWritingTo: URL(fileURLWithPath: path))
        defer { try? handle.close() }
        for index in 0..<count {
            let segment = URL(fileURLWithPath: "\(directory)/\(index)")
            try handle.write(contentsOf: Data(contentsOf: segment, options: .mappedIfSafe))
        }
    }

    private static func removeSegmentFiles(of transfer: YoutubeTransfer) {
        guard let segmentsPath = transfer.segmentsPath else { return }
        try? FileManager.default.removeItem(atPath: transfer.segmentDirectory)
        try? FileManager.default.removeItem(atPath: segmentsPath)
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
            // 기본값은 0644 다. 전송이 통짜였을 때 OS 임시 파일을 옮겨 받던 권한과 맞춘다.
            manager.createFile(atPath: path, contents: nil, attributes: [.posixPermissions: 0o600])
        }

        let handle = try FileHandle(forWritingTo: URL(fileURLWithPath: path))
        defer { try? handle.close() }
        try handle.truncate(atOffset: UInt64(totalBytes))
        try handle.seek(toOffset: UInt64(offset))
        try handle.write(contentsOf: Data(contentsOf: location, options: .mappedIfSafe))
    }
}
