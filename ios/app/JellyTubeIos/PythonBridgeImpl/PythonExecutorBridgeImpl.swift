import Foundation
import PythonKit
import shared

enum PythonExecutorBridgeError: Error {
    case invalidDownloadResult
    case invalidPlaylistResult
}

final class PythonExecutorBridgeImpl: IosPythonExecutorBridge {
    private let runtime = PythonRuntime()

    func __downloadYoutubeMedia(
        youtubeUrl: String,
        audioDownloadPath: String,
        completionHandler: @escaping @Sendable (YoutubeDownloadDto?, Error?) -> Void,
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.runtime.callFunction(
                    moduleName: Self.fileName,
                    functionName: Self.methodDownloadAudio,
                    arguments: [youtubeUrl, audioDownloadPath]
                )

                guard result.count >= 5 else {
                    throw PythonExecutorBridgeError.invalidDownloadResult
                }

                completionHandler(
                    YoutubeDownloadDto(
                        title: result[safe: 0] ?? "알 수 없음",
                        thumbnailDownloadUrl: result[safe: 1] ?? "",
                        duration: result[safe: 2] ?? "0",
                        uploader: result[safe: 3] ?? "알 수 없음",
                        description: result[safe: 4] ?? ""
                    ),
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

    private static let fileName = "download_youtube_audio"
    private static let methodDownloadAudio = "download_audio"
    private static let methodExtractVideoUrlsFromPlaylist = "extract_video_urls_from_playlist"
}

private final class PythonRuntime {
    private var didConfigurePython = false

    func callFunction(
        moduleName: String,
        functionName: String,
        arguments: [String]
    ) throws -> [String] {
        configurePythonIfNeeded()

        let module = Python.import(moduleName)
        let function = module[dynamicMember: functionName]
        let result = function.dynamicallyCall(withArguments: arguments.map(PythonObject.init))

        guard let list = Array<String>(result) else {
            throw PythonExecutorBridgeError.invalidPlaylistResult
        }
        return list
    }

    private func configurePythonIfNeeded() {
        guard !didConfigurePython else { return }

        if let pythonLibraryPath {
            PythonLibrary.useLibrary(at: pythonLibraryPath)
        }

        let sys = Python.import("sys")
        sys.path.insert(0, PythonObject(pythonSourcePath()))
        didConfigurePython = true
    }

    private var pythonLibraryPath: String? {
        let candidates = [
            Bundle.main.privateFrameworksPath.map { "\($0)/Python.framework/Python" },
            Bundle.main.path(forResource: "Python", ofType: "framework").map { "\($0)/Python" },
        ].compactMap { $0 }
        return candidates.first
    }

    private func pythonSourcePath() -> String {
        let bundlePath = Bundle.main.bundlePath
        let processPath = ProcessInfo.processInfo.environment["PWD"]
        let candidates = [
            processPath?.components(separatedBy: "/ios/app").first.map { "\($0)/shared/data/src/python" },
            "\(bundlePath)/shared/data/src/python",
            "\(bundlePath)/src/python",
        ].compactMap { $0 }
        return candidates.first ?? bundlePath
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
