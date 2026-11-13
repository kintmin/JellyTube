package com.kintmin.ios.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import com.kintmin.log.model.analyticsUserId
import com.kintmin.log.model.toAnalyticsEvent

/**
 * `android:app` 의 `AppLogImpl` 에 대응하는 iOS 구현.
 *
 * GA4 제약으로 자르는 쪽이 Swift 가 아니라 여기인 것은 두 플랫폼이 같은 값을 보내야 해서다.
 * https://support.google.com/analytics/answer/9267744
 */
internal class IosAppLog(
    private val bridge: IosAppLogBridge,
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        val trimmed = analyticsUserId(userId)
        bridge.setUserId(trimmed)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$trimmed")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        bridge.debug(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    override fun sendFirebaseEvent(firebaseEvent: FirebaseEvent) {
        val event = firebaseEvent.toAnalyticsEvent()
        val logName = event.name
        val params = event.params

        bridge.event(logName, params)
        appLogRepository.appendAppLog(
            "FIREBASE_EVENT",
            "$logName {${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}}",
        )
    }
}
