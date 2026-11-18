package com.kintmin.ios.adapter

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import com.kintmin.log.model.analyticsUserId
import com.kintmin.log.model.toAnalyticsEvent

/**
 * GA4 제약에 맞춰 자른다.
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
