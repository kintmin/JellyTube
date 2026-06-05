import SwiftUI
import shared

@main
struct JellyTubeIosApp: App {
    init() {
        IosPlaylistUseCaseBridgeKt.doInitIosKoin(
            pythonExecutorBridge: PythonExecutorBridgeImpl()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
