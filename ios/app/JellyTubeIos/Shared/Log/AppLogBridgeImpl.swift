import Foundation
import os
import shared

/// 로그 수집기로 나가는 유일한 자리. 공유 코드(`IosAppLog`)는 자르기와 파일 기록만 한다.
/// 수집기 SDK 를 붙이면 아래 `// MARK: 수집기` 세 자리에 호출을 더한다.
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
