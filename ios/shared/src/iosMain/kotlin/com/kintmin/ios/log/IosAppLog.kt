package com.kintmin.ios.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent

/**
 * `android:app` 의 `AppLogImpl` 에 대응하는 iOS 구현.
 *
 * 하는 일은 셋뿐이다 — **GA4 제약으로 자르고**, 파일 로그에 남기고, 수집기 창구로 넘긴다.
 * 자르는 규칙을 Kotlin 에 두는 이유는 두 플랫폼이 같은 값을 보내야 하기 때문이다.
 * Swift 에 맡기면 한쪽만 규칙이 바뀌는 날이 온다.
 *
 * [GA4 제약사항] 이벤트명 40자 / param key 40자 / param value 100자 / param 25개
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
                // 값이 없는 param 은 보내지 않는다. Android 의 bundleOf 도 null 을 싣지 않는다.
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
