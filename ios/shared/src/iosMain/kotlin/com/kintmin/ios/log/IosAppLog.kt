package com.kintmin.ios.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent

/**
 * `android:app` 의 `AppLogImpl` 에 대응하는 iOS 구현. 자르는 규칙은 `IosAppLogTest` 가 고정한다.
 *
 * 자르는 쪽이 Swift 가 아니라 여기인 것은 두 플랫폼이 같은 값을 보내야 해서다.
 * https://support.google.com/analytics/answer/9267744
 */
internal class IosAppLog(
    private val bridge: IosAppLogBridge,
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        val trimmed = userId.take(MAX_USER_ID_LENGTH)
        bridge.setUserId(trimmed)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$trimmed")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        bridge.debug(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    override fun sendFirebaseEvent(firebaseEvent: FirebaseEvent) {
        val logName = firebaseEvent.logName.name.take(MAX_NAME_LENGTH)
        val params = firebaseEvent.params
            .take(MAX_PARAM_COUNT)
            .mapNotNull { (key, value) ->
                value?.let { key.name.take(MAX_NAME_LENGTH) to it.take(MAX_VALUE_LENGTH) }
            }
            .toMap()

        bridge.event(logName, params)
        appLogRepository.appendAppLog(
            "FIREBASE_EVENT",
            "$logName {${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}}",
        )
    }

    private companion object {
        const val MAX_USER_ID_LENGTH = 256
        const val MAX_NAME_LENGTH = 40
        const val MAX_VALUE_LENGTH = 100
        const val MAX_PARAM_COUNT = 25
    }
}
