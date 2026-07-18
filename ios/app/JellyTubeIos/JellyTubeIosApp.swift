import SwiftUI
import shared

@main
struct JellyTubeIosApp: App {
    init() {
        IosKoinKt.doInitIosKoin(
            pythonExecutorBridge: PythonExecutorBridgeImpl()
        )
    }

    var body: some Scene {
        WindowGroup {
            // UI 재구성 전 임시 placeholder. Koin/AVPlayer/SKIE 인프라는 그대로 살아 있음.
            Text("JellyTube")
        }
    }
}
