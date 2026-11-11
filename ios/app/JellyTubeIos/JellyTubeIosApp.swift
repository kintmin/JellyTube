import SwiftUI
import UserNotifications
import shared

/// 백그라운드 전송이 끝나 OS 가 앱을 깨울 때를 받는다.
/// 이 자리가 비어 있으면 지난 생애에 끝난 전송의 완료 콜백이 오지 않는다.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 이 델리게이트가 없으면 앱을 보고 있는 동안 띄운 로컬 알림이 조용히 버려진다.
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        YoutubeDownloader.shared.backgroundCompletionHandler = completionHandler
        YoutubeDownloader.shared.activate()
    }
}

@main
struct JellyTubeIosApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // 수집기는 Koin 보다 먼저 선다. 이 앞에서 나는 크래시는 받는 곳이 없다.
        let appLogBridge = AppLogBridgeImpl()

        // 순서가 중요하다 — Koin 이 먼저 서야 startApp 이 앱 레벨 ViewModel 을 꺼낼 수 있다.
        IosKoinKt.doInitIosKoin(
            pythonExecutorBridge: PythonExecutorBridgeImpl(),
            mediaControllerBridge: MediaControllerBridgeImpl(),
            appLogBridge: appLogBridge,
            migrationSourceBrowserBridge: MigrationSourceBrowserBridgeImpl()
        )
        // 지난 생애의 완료 콜백을 받으려면 세션을 먼저 되살려야 한다.
        YoutubeDownloader.shared.activate()
        Self.clearSharedFileInbox()
        MainViewControllerKt.startApp()
    }

    /// "JellyTube 로 열기"로 들어온 파일은 iOS 가 `Documents/Inbox/` 에 복사해 둔다.
    /// 앱이 자기 저장소로 다시 복사해 들이므로 그 원본은 쓸모가 없고, 치우지 않으면 쌓인다.
    /// 들여오는 중에 지울 수는 없으니 다음 기동에 치운다.
    private static func clearSharedFileInbox() {
        guard let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        else { return }

        let inbox = documents.appendingPathComponent("Inbox")
        guard let entries = try? FileManager.default.contentsOfDirectory(
            at: inbox,
            includingPropertiesForKeys: nil
        ) else { return }

        for entry in entries {
            try? FileManager.default.removeItem(at: entry)
        }
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                // ComposeUIViewController 가 safe area 를 직접 Compose 로 넘기므로 화면 전체를
                // 덮어야 한다. 인셋 처리는 각 화면의 Scaffold(innerPadding)가 한다.
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    // 판정은 공통 코드가 한다. Swift 는 "파일이냐 아니냐"만 가른다.
                    if url.isFileURL {
                        MainViewControllerKt.handleSharedAudioFile(url: url.absoluteString)
                    } else {
                        MainViewControllerKt.handleDeepLink(url: url.absoluteString)
                    }
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
