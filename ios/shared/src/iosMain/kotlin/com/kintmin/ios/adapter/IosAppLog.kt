package com.kintmin.ios.adapter

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.SentryEvent

internal class IosAppLog(
    private val bridge: IosAppLogBridge,
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        bridge.setUserId(userId)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$userId")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        bridge.debug(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    override fun sendSentryEvent(sentryEvent: SentryEvent) {
        val logName = sentryEvent.logName.name
        val params = sentryEvent.attributes

        bridge.event(logName, params)
        appLogRepository.appendAppLog(
            "FIREBASE_EVENT",
            "$logName {${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}}",
        )
    }
}
