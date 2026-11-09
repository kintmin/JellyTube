import SwiftUI
import shared

@main
struct JellyTubeIosApp: App {

    init() {
        // 순서가 중요하다 — Koin 이 먼저 서야 startApp 이 앱 레벨 ViewModel 을 꺼낼 수 있다.
        IosKoinKt.doInitIosKoin(
            pythonExecutorBridge: PythonExecutorBridgeImpl(),
            mediaControllerBridge: MediaControllerBridgeImpl()
        )
        MainViewControllerKt.startApp()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                // ComposeUIViewController 가 safe area 를 직접 Compose 로 넘기므로 화면 전체를
                // 덮어야 한다. 인셋 처리는 각 화면의 Scaffold(innerPadding)가 한다.
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    // 판정은 공통 코드가 한다. Swift 는 주소만 넘긴다.
                    MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                }
        }
    }
}

private struct ComposeRootView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
