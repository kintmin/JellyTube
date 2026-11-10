import Foundation
import os
import shared

/// 로그 수집기로 나가는 유일한 자리.
///
/// 공유 코드(`IosAppLog`)는 자르기와 파일 기록만 하고, 실제 수집기 호출은 전부 여기다.
/// **수집기를 Firebase 에서 Sentry 로 바꾸더라도 고칠 파일은 이것 하나다.**
///
/// 지금은 `os.Logger` 로만 내보낸다. Firebase iOS SDK 를 붙이려면
/// ① SPM 으로 `firebase-ios-sdk`(Analytics + Crashlytics) 추가
/// ② `GoogleService-Info.plist` 를 번들에 넣고
/// ③ 앱이 뜨는 **가장 앞**(Koin 보다 먼저)에서 `FirebaseApp.configure()`
/// 세 가지가 선행된다. ②는 Firebase 콘솔에서 iOS 앱을 등록해야 나오는 파일이라
/// 코드만으로 끝나지 않는다 — 아래 `// MARK: 수집기` 표시한 세 자리에 호출만 더하면 된다.
final class AppLogBridgeImpl: IosAppLogBridge {

    private let logger = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "AppLog")

    func setUserId(userId: String) {
        logger.info("setUserId=\(userId, privacy: .public)")
        // MARK: 수집기 — Crashlytics.crashlytics().setUserID(userId); Analytics.setUserID(userId)
    }

    func debug(tag: String, message: String) {
        logger.debug("[\(tag, privacy: .public)] \(message, privacy: .public)")
        // MARK: 수집기 — Crashlytics.crashlytics().log("[\(tag)] \(message)")
    }

    func event(name: String, params: [String: String]) {
        let formatted = params.map { "\($0.key)=\($0.value)" }.sorted().joined(separator: ", ")
        logger.info("\(name, privacy: .public) {\(formatted, privacy: .public)}")
        // MARK: 수집기 — Analytics.logEvent(name, parameters: params)
    }
}
