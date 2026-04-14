package com.kintmin.log_impl

import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.take

@Singleton
class AppLogImpl @Inject constructor(
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        val userId = userId.take(256)
        FirebaseCrashlytics.getInstance().setUserId(userId)
        Firebase.analytics.setUserId(userId)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$userId")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        android.util.Log.d(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    /**
     * [GA4 제약사항]
     * 이벤트명 제한: 최대 길이는 영문 기준 40자
     * param key 제한: 최대 길이는 영문 기준 40자
     * param value 제한: 최대 길이는 영문 기준 100자
     * param 개수 제한: 한 로그 당 params 최대 개수는 25개
     * https://support.google.com/analytics/answer/9267744
     */
    override fun sendFirebaseEvent(firebaseEvent: FirebaseEvent) {
        val logName = firebaseEvent.logName.name.take(40)

        val pairs = firebaseEvent.params.map {
            it.first.name.take(40) to it.second?.take(100)
        }.take(25).toTypedArray()

        val params = bundleOf(*pairs)

        if (BuildConfig.DEBUG) {
            val logMessage = if (!params.isEmpty) {
                val formattedBundle = params.keySet().joinToString(separator = "\n\t") { key ->
                    "$key=${params.getString(key)}"
                }
                "${logName}\n\t$formattedBundle"
            } else {
                logName
            }

            android.util.Log.d("JellyTubeFirebaseEvent", logMessage)
        }

        Firebase.analytics.logEvent(logName, params)
        val paramLog = params.keySet().joinToString(", ") { key -> "$key=${params.getString(key)}" }
        appLogRepository.appendAppLog("FIREBASE_EVENT", "$logName {$paramLog}")
    }
}
