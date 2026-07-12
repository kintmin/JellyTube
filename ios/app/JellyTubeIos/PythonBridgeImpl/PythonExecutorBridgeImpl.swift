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

        return convertPythonSequenceToStrings(result)
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

    private func configurePythonIfNeeded() {
        guard !didConfigurePython else { return }

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
            "\(bundlePath)/python",
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
