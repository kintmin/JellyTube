import Foundation
import os

private let downloadLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "YoutubeDownload")

/// 전송 자체는 끝났지만 받은 것이 음원이 아닐 때.
enum YoutubeDownloadError: LocalizedError {
    /// 서명이 만료됐거나 헤더가 달라 유튜브가 거부한 경우. 본문이 오류 페이지라 파일이 생긴다.
    case httpStatus(Int)
    /// 200 이지만 본문이 비어 있는 경우.
    case emptyBody

    var errorDescription: String? {
        switch self {
        case .httpStatus(let code): return "음원 주소가 거부됐습니다 (HTTP \(code))"
        case .emptyBody: return "받은 음원이 비어 있습니다"
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

/// 유튜브 음원을 background `URLSession` 으로 내려받는다.
///
/// 전송 주체가 앱 프로세스가 아니라 **OS 데몬**이라 앱이 백그라운드로 내려가거나 시스템이
/// 앱을 정리해도 이어진다. 대신 사용자가 앱 스위처에서 강제 종료하면 OS 가 전부 취소한다 —
/// 그건 정책이라 우회할 방법이 없다.
///
/// 완료가 **앱의 다음 생애에** 올 수 있어 진행 중인 전송을 디스크에 적어 둔다. 기다리던
/// 쪽이 없으면 받은 파일을 목적지에 옮겨만 두고, 다음에 같은 주소로 요청이 오면 전송 없이
/// 그 결과를 돌려준다([takeCompleted]).
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
    private var waiters: [Int: (Error?) -> Void] = [:]

    private override init() {
        super.init()
    }

    /// 앱이 뜰 때 한 번 불러 세션을 되살린다. 그래야 지난 생애의 완료 콜백이 도착한다.
    func activate() {
        _ = session
    }

    /// 지난 생애에 끝난 전송이 있으면 꺼내 간다. 꺼낸 기록은 지운다.
    func takeCompleted() -> [YoutubeAudioSource] {
        lock.lock()
        defer { lock.unlock() }

        let completed = Self.load(key: Self.completedKey)
        if !completed.isEmpty {
            UserDefaults.standard.removeObject(forKey: Self.completedKey)
        }
        return Array(completed.values)
    }

    /// 같은 주소의 결과가 이미 디스크에 있으면 돌려준다. 전송을 다시 하지 않기 위해서다.
    func consumeCompleted(sourceUrl: String) -> YoutubeAudioSource? {
        lock.lock()
        defer { lock.unlock() }

        var completed = Self.load(key: Self.completedKey)
        guard let source = completed.removeValue(forKey: sourceUrl) else { return nil }
        guard FileManager.default.fileExists(atPath: source.destinationPath) else { return nil }

        Self.save(completed, key: Self.completedKey)
        return source
    }

    func download(
        from remoteUrl: URL,
        headers: [String: String],
        source: YoutubeAudioSource,
        completion: @escaping (Error?) -> Void
    ) {
        var request = URLRequest(url: remoteUrl)
        // 유튜브가 주는 주소는 서명된 단기 주소다. 추출 때와 다른 헤더로 요청하면 거부될 수 있어
        // yt-dlp 가 준 헤더를 그대로 싣는다.
        for (field, value) in headers {
            request.setValue(value, forHTTPHeaderField: field)
        }

        let task = session.downloadTask(with: request)

        lock.lock()
        waiters[task.taskIdentifier] = completion
        var inFlight = Self.load(key: Self.inFlightKey)
        inFlight[String(task.taskIdentifier)] = source
        Self.save(inFlight, key: Self.inFlightKey)
        lock.unlock()

        downloadLog.info("start task=\(task.taskIdentifier) dest=\(source.destinationPath, privacy: .public)")
        task.resume()
    }

    // MARK: - 기록

    private static func load(key: String) -> [String: YoutubeAudioSource] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([String: YoutubeAudioSource].self, from: data)
        else { return [:] }
        return decoded
    }

    private static func save(_ value: [String: YoutubeAudioSource], key: String) {
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
        lock.lock()
        var inFlight = Self.load(key: Self.inFlightKey)
        let source = inFlight.removeValue(forKey: String(downloadTask.taskIdentifier))
        Self.save(inFlight, key: Self.inFlightKey)
        let waiter = waiters[downloadTask.taskIdentifier]
        lock.unlock()

        guard let source else {
            downloadLog.error("no record for task=\(downloadTask.taskIdentifier)")
            return
        }

        // `URLSessionDownloadTask` 는 403 도 "전송 성공"으로 본다 — 오류 페이지를 파일로
        // 내려 줄 뿐이다. 여기서 걸러내지 않으면 0바이트 m4a 가 DB 에 등록된다.
        let statusCode = (downloadTask.response as? HTTPURLResponse)?.statusCode ?? 0
        let attributes = try? FileManager.default.attributesOfItem(atPath: location.path)
        let byteCount = (attributes?[.size] as? NSNumber)?.intValue ?? 0
        downloadLog.info("finish task=\(downloadTask.taskIdentifier) status=\(statusCode) bytes=\(byteCount)")

        guard (200...299).contains(statusCode) else {
            finish(
                taskIdentifier: downloadTask.taskIdentifier,
                waiter: waiter,
                error: YoutubeDownloadError.httpStatus(statusCode),
                source: nil
            )
            return
        }
        guard byteCount > 0 else {
            finish(
                taskIdentifier: downloadTask.taskIdentifier,
                waiter: waiter,
                error: YoutubeDownloadError.emptyBody,
                source: nil
            )
            return
        }

        // 이 함수가 끝나면 OS 가 임시 파일을 지운다. 반드시 여기서 옮겨야 한다.
        let destination = URL(fileURLWithPath: source.destinationPath)
        do {
            try FileManager.default.createDirectory(
                at: destination.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            if FileManager.default.fileExists(atPath: destination.path) {
                try FileManager.default.removeItem(at: destination)
            }
            try FileManager.default.moveItem(at: location, to: destination)
        } catch {
            downloadLog.error("move failed: \(error.localizedDescription, privacy: .public)")
            finish(taskIdentifier: downloadTask.taskIdentifier, waiter: waiter, error: error, source: nil)
            return
        }

        finish(taskIdentifier: downloadTask.taskIdentifier, waiter: waiter, error: nil, source: source)
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let error else { return }  // 성공은 didFinishDownloadingTo 가 이미 처리했다

        lock.lock()
        var inFlight = Self.load(key: Self.inFlightKey)
        inFlight.removeValue(forKey: String(task.taskIdentifier))
        Self.save(inFlight, key: Self.inFlightKey)
        let waiter = waiters.removeValue(forKey: task.taskIdentifier)
        lock.unlock()

        downloadLog.error("task=\(task.taskIdentifier) failed: \(error.localizedDescription, privacy: .public)")
        waiter?(error)
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        DispatchQueue.main.async {
            self.backgroundCompletionHandler?()
            self.backgroundCompletionHandler = nil
        }
    }

    /// 기다리던 쪽이 있으면 바로 알리고, 없으면(앱이 새로 뜬 경우) 결과를 적어 둔다.
    private func finish(
        taskIdentifier: Int,
        waiter: ((Error?) -> Void)?,
        error: Error?,
        source: YoutubeAudioSource?
    ) {
        lock.lock()
        waiters.removeValue(forKey: taskIdentifier)
        if waiter == nil, let source {
            var completed = Self.load(key: Self.completedKey)
            completed[source.sourceUrl] = source
            Self.save(completed, key: Self.completedKey)
        }
        lock.unlock()

        waiter?(error)
    }
}
