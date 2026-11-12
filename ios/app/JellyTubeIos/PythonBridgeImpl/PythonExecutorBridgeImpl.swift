import Foundation
import PythonKit
import shared

enum PythonExecutorBridgeError: LocalizedError {
    case invalidDownloadResult
    case invalidPlaylistResult
    case invalidDirectUrl
    /// `PythonKit.PythonError` 는 `localizedDescription` 이 "error 0" 라 쓸모가 없다.
    /// 파이썬이 뭐라고 던졌는지 로그에 남기려면 여기서 문자열로 바꿔 다시 올려야 한다.
    case python(String)

    var errorDescription: String? {
        switch self {
        case .invalidDownloadResult: return "다운로드 결과를 읽을 수 없습니다"
        case .invalidPlaylistResult: return "재생목록 결과를 읽을 수 없습니다"
        case .invalidDirectUrl: return "음원 주소를 찾지 못했습니다"
        case .python(let message): return message
        }
    }
}

final class PythonExecutorBridgeImpl: IosPythonExecutorBridge {
    private let runtime = PythonRuntime()

    /// 추출과 전송을 가른다.
    ///
    /// yt-dlp 는 주소만 알아내고, 바이트를 받는 것은 background `URLSession` 이 맡는다.
    /// Python 안에서 받으면 전송 주체가 앱 프로세스라 앱이 멈추면 같이 멈춘다.
    func __downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
        completionHandler: @escaping @Sendable (YoutubeDownloadDto?, Error?) -> Void,
    ) {
        // 지난 생애에 이미 끝난 전송이면 다시 받지 않는다.
        if let finished = YoutubeDownloader.shared.consumeCompleted(sourceUrl: youtubeUrl) {
            completionHandler(finished.toDto(), nil)
            return
        }

        attemptDownload(
            youtubeUrl: youtubeUrl,
            audioDownloadPath: audioDownloadPath,
            remainingRetryCount: Self.extractRetryCount,
            completionHandler: completionHandler
        )
    }

    /// 추출 → 전송을 한 번 시도하고, 전송이 거부되면 다시 추출해서 재시도한다.
    ///
    /// 유튜브가 주는 주소는 서명된 단기 주소다. 같은 주소로 다시 쏘는 것은 의미가 없고
    /// 추출부터 다시 해야 새 서명을 받는다.
    private func attemptDownload(
        youtubeUrl: String,
        audioDownloadPath: String,
        remainingRetryCount: Int,
        completionHandler: @escaping @Sendable (YoutubeDownloadDto?, Error?) -> Void
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            // 추출이 끝나고 전송이 등록될 때까지 앱이 돌아야 한다.
            YoutubeDownloader.shared.beginExtraction()
            defer { YoutubeDownloader.shared.endExtraction() }

            do {
                let extractStart = Date()
                let result = try self.runtime.callFunction(
                    moduleName: Self.fileName,
                    functionName: Self.methodExtractAudioSource,
                    arguments: [youtubeUrl]
                )
                SwiftAppLogKt.swiftDebugLog(
                    tag: "YoutubeExtract",
                    message: String(format: "elapsed=%.2fs", Date().timeIntervalSince(extractStart))
                )

                guard result.count >= 8, let remoteUrl = URL(string: result[0]) else {
                    throw PythonExecutorBridgeError.invalidDirectUrl
                }

                let ext = result[safe: 2].flatMap { $0.isEmpty ? nil : $0 } ?? "m4a"
                let source = YoutubeAudioSource(
                    sourceUrl: youtubeUrl,
                    destinationPath: "\(audioDownloadPath).\(ext)",
                    fileNameWithExt: "\((audioDownloadPath as NSString).lastPathComponent).\(ext)",
                    title: result[safe: 3].flatMap { $0.isEmpty ? nil : $0 } ?? "알 수 없음",
                    thumbnailUrl: result[safe: 4] ?? "",
                    duration: result[safe: 5].flatMap { $0.isEmpty ? nil : $0 } ?? "0",
                    uploader: result[safe: 6].flatMap { $0.isEmpty ? nil : $0 } ?? "알 수 없음",
                    description: result[safe: 7] ?? ""
                )

                YoutubeDownloader.shared.download(
                    from: remoteUrl,
                    headers: Self.parseHeaders(result[1]),
                    // 크기를 미리 알면 구간 전부를 앱이 떠 있는 동안 등록할 수 있다.
                    totalBytes: result[safe: 8].flatMap { Int64($0) },
                    source: source
                ) { error in
                    guard let error else {
                        completionHandler(source.toDto(), nil)
                        return
                    }

                    if remainingRetryCount > 0, Self.isWorthReextracting(error) {
                        self.attemptDownload(
                            youtubeUrl: youtubeUrl,
                            audioDownloadPath: audioDownloadPath,
                            remainingRetryCount: remainingRetryCount - 1,
                            completionHandler: completionHandler
                        )
                    } else {
                        completionHandler(nil, error as NSError)
                    }
                }
            } catch {
                completionHandler(nil, error as NSError)
            }
        }
    }

    /// 네트워크 단절·디스크 오류는 다시 추출해도 같은 자리에서 넘어지므로 바로 올린다.
    private static func isWorthReextracting(_ error: Error) -> Bool {
        guard case .httpStatus(let code) = error as? YoutubeDownloadError else { return false }
        return code == 403 || code == 404 || code == 410
    }

    /// 데이터 이전 중 다시 받기용. 앱이 떠 있는 동안만 쓰므로 background `URLSession` 을 거치지 않는다.
    /// 그 대신 yt-dlp 가 HLS 도 직접 받아, 단일 파일 m4a 가 없는 영상도 받을 수 있다.
    func __downloadPlayableAudio(
        youtubeUrl: String,
        audioDownloadPath: String,
        completionHandler: @escaping @Sendable (PlayableAudioDownloadDto?, Error?) -> Void
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.runtime.callFunction(
                    moduleName: Self.fileName,
                    functionName: Self.methodDownloadPlayableAudio,
                    arguments: [youtubeUrl, audioDownloadPath]
                )
                guard let fileNameWithExt = result[safe: 0], !fileNameWithExt.isEmpty else {
                    throw PythonExecutorBridgeError.invalidDownloadResult
                }
                completionHandler(
                    PlayableAudioDownloadDto(audioFileNameWithExt: fileNameWithExt, duration: result[safe: 1] ?? ""),
                    nil
                )
            } catch {
                completionHandler(nil, error as NSError)
            }
        }
    }

    func __extractYoutubeUrlsFromPlaylist(
        playlistUrl: String,
        completionHandler: @escaping @Sendable ([String]?, Error?) -> Void,
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.runtime.callFunction(
                    moduleName: Self.fileName,
                    functionName: Self.methodExtractVideoUrlsFromPlaylist,
                    arguments: [playlistUrl]
                )
                completionHandler(result, nil)
            } catch {
                completionHandler(nil, error as NSError)
            }
        }
    }

    func __transliterateToKorean(
        text: String,
        completionHandler: @escaping @Sendable (String?, Error?) -> Void,
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.runtime.callFunctionReturningString(
                    moduleName: Self.transliterateFileName,
                    functionName: Self.methodTransliterateToKorean,
                    arguments: [text]
                )
                completionHandler(result, nil)
            } catch {
                completionHandler(nil, error as NSError)
            }
        }
    }

    func takeFinishedDownloadUrls() -> [String] {
        YoutubeDownloader.shared.finishedSourceUrls()
    }

    private static let fileName = "download_youtube_audio"
    private static let methodDownloadAudio = "download_audio"
    private static let methodExtractVideoUrlsFromPlaylist = "extract_video_urls_from_playlist"
    private static let methodExtractAudioSource = "extract_audio_source"
    private static let methodDownloadPlayableAudio = "download_playable_audio"

    /// 재추출 재시도 횟수. 계속 거부되는 것은 주소가 상한 것이 아니라 그 영상을 받을 수 없는 것이다.
    private static let extractRetryCount = 1

    private static func parseHeaders(_ json: String) -> [String: String] {
        guard let data = json.data(using: .utf8),
              let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: String]
        else { return [:] }
        return parsed
    }
    private static let transliterateFileName = "transliterate_lyrics"
    private static let methodTransliterateToKorean = "transliterate_to_korean"
}

/// PythonKit 은 GIL 을 다루지 않는다. 파이썬 호출은 전부 `withGIL` 안에서 한다.
final class PythonRuntime {
    // 인터프리터는 프로세스에 하나라 인스턴스가 아니라 타입이 한 번 초기화한다.
    // `Py_Initialize` 는 GIL 을 쥔 채 돌아온다. 놓지 않으면 다른 스레드의 `PyGILState_Ensure` 가
    // 영영 기다린다. `configurePython` 의 `PythonObject` 가 다 풀린 뒤라야 해서 함수를 나눴다.
    private static let configured: Void = {
        configurePython()
        PythonGIL.releaseAfterInitialize()
    }()

    func callFunction(
        moduleName: String,
        functionName: String,
        arguments: [String]
    ) throws -> [String] {
        try withGIL {
            let result = try call(moduleName: moduleName, functionName: functionName, arguments: arguments)
            return convertPythonSequenceToStrings(result)
        }
    }

    /// 문자열 하나를 돌려주는 함수용. 시퀀스 변환을 쓰면 문자열이 글자 단위로 쪼개진다.
    func callFunctionReturningString(
        moduleName: String,
        functionName: String,
        arguments: [String]
    ) throws -> String {
        try withGIL {
            let result = try call(moduleName: moduleName, functionName: functionName, arguments: arguments)
            return String(result) ?? String(Python.str(result)) ?? ""
        }
    }

    /// `PythonObject` 를 밖으로 내보내지 않는다. 해제가 `Py_DecRef` 라 GIL 밖에서 풀리면 죽는다.
    private func withGIL<T>(_ body: () throws -> T) rethrows -> T {
        _ = Self.configured
        let state = PythonGIL.ensure()
        defer { PythonGIL.release(state) }
        return try body()
    }

    /// `Python.import` 와 `dynamicallyCall` 은 파이썬 예외에서 **트랩한다**. throwing 변형을 써야
    /// 예외가 Swift 에러로 올라와 실패 알림으로 떨어진다.
    private func call(
        moduleName: String,
        functionName: String,
        arguments: [String]
    ) throws -> PythonObject {
        do {
            let module = try Python.attemptImport(moduleName)
            let function = module[dynamicMember: functionName]
            // 타입을 반드시 명시한다. `ThrowingPythonObject` 에는 가변인자 오버로드가 같이 있어서,
            // 배열을 그대로 넘기면 그 오버로드가 골라져 **인자 하나짜리 리스트**로 싸인다.
            let positionalArguments: [PythonConvertible] = arguments.map { PythonObject($0) }
            return try function.throwing.dynamicallyCall(withArguments: positionalArguments)
        } catch {
            throw PythonExecutorBridgeError.python("\(moduleName).\(functionName): \(error)")
        }
    }

    // Python이 반환하는 튜플/리스트가 str/int/None 혼합일 수 있어 각 원소를 문자열로 관대하게 변환한다.
    private func convertPythonSequenceToStrings(_ result: PythonObject) -> [String] {
        guard let count = Int(Python.len(result)) else { return [] }
        var list: [String] = []
        list.reserveCapacity(count)
        for index in 0..<count {
            let item = result[PythonObject(index)]
            if item == Python.None {
                list.append("")
            } else if let str = String(item) {
                list.append(str)
            } else {
                list.append(String(Python.str(item)) ?? "")
            }
        }
        return list
    }

    private static func configurePython() {
        let bundlePath = Bundle.main.bundlePath
        let pythonHome = "\(bundlePath)/python"
        let appPackages = "\(bundlePath)/app-packages"

        setenv("PYTHONHOME", pythonHome, 1)
        setenv("PYTHONPATH", "\(pythonHome):\(appPackages)", 1)
        setenv("PYTHONDONTWRITEBYTECODE", "1", 1)
        setenv("PYTHONUNBUFFERED", "1", 1)

        if let libPath = pythonLibraryPath,
           FileManager.default.fileExists(atPath: libPath) {
            PythonLibrary.useLibrary(at: libPath)
        }

        let sys = Python.import("sys")
        sys.path.insert(0, PythonObject(pythonHome))
        sys.path.insert(0, PythonObject(appPackages))
        sys.path.insert(0, PythonObject(pythonSourcePath()))

        // 번들 파이썬에는 루트 인증서가 없다. 세우지 않으면 모든 https 요청이
        // CERTIFICATE_VERIFY_FAILED 로 떨어진다.
        let caBundle = "\(appPackages)/certifi/cacert.pem"
        if FileManager.default.fileExists(atPath: caBundle) {
            setenv("SSL_CERT_FILE", caBundle, 1)
            setenv("REQUESTS_CA_BUNDLE", caBundle, 1)
        }
    }

    private static var pythonLibraryPath: String? {
        let candidates = [
            Bundle.main.privateFrameworksPath.map { "\($0)/Python.framework/Python" },
            Bundle.main.path(forResource: "Python", ofType: "framework").map { "\($0)/Python" },
        ].compactMap { $0 }
        return candidates.first
    }

    private static func pythonSourcePath() -> String {
        let bundlePath = Bundle.main.bundlePath
        let processPath = ProcessInfo.processInfo.environment["PWD"]
        let candidates = [
            "\(bundlePath)/python",
            processPath?.components(separatedBy: "/ios/app").first.map { "\($0)/shared/data/src/python" },
            "\(bundlePath)/shared/data/src/python",
            "\(bundlePath)/src/python",
        ].compactMap { $0 }
        return candidates.first ?? bundlePath
    }
}

/// PythonKit 이 노출하지 않는 GIL C API. Python.xcframework 가 링크돼 있어 전역 심볼로 찾는다.
private enum PythonGIL {
    private typealias EnsureFunction = @convention(c) () -> Int32
    private typealias ReleaseFunction = @convention(c) (Int32) -> Void
    private typealias SaveThreadFunction = @convention(c) () -> OpaquePointer?

    private static let ensureFunction: EnsureFunction = load("PyGILState_Ensure")
    private static let releaseFunction: ReleaseFunction = load("PyGILState_Release")
    private static let saveThreadFunction: SaveThreadFunction = load("PyEval_SaveThread")

    static func ensure() -> Int32 { ensureFunction() }

    static func release(_ state: Int32) { releaseFunction(state) }

    static func releaseAfterInitialize() { _ = saveThreadFunction() }

    private static func load<T>(_ name: String) -> T {
        // RTLD_DEFAULT. 매크로라 Swift 로 들어오지 않는다.
        guard let symbol = dlsym(UnsafeMutableRawPointer(bitPattern: -2), name) else {
            fatalError("\(name) 를 찾을 수 없습니다")
        }
        return unsafeBitCast(symbol, to: T.self)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

private extension YoutubeAudioSource {
    func toDto() -> YoutubeDownloadDto {
        YoutubeDownloadDto(
            title: title,
            thumbnailDownloadUrl: thumbnailUrl,
            duration: duration,
            uploader: uploader,
            description: description,
            audioFileNameWithExt: fileNameWithExt
        )
    }
}
