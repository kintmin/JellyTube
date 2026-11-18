package com.kintmin.jellytube.log_impl

import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.jellytube.BuildConfig
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent
import com.kintmin.log.model.analyticsUserId
import com.kintmin.log.model.toAnalyticsEvent

class AppLogImpl(
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        val userId = analyticsUserId(userId)
        FirebaseCrashlytics.getInstance().setUserId(userId)
        Firebase.analytics.setUserId(userId)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$userId")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        Log.d(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    override fun sendFirebaseEvent(firebaseEvent: FirebaseEvent) {
        val event = firebaseEvent.toAnalyticsEvent()
        val logName = event.name
        val params = bundleOf(*event.params.toList().toTypedArray())

        if (BuildConfig.DEBUG) {
            val logMessage = if (!params.isEmpty) {
                val formattedBundle = params.keySet().joinToString(separator = "\n\t") { key ->
                    "$key=${params.getString(key)}"
                }
                "${logName}\n\t$formattedBundle"
            } else {
                logName
            }

            Log.d("JellyTubeFirebaseEvent", logMessage)
        }

        Firebase.analytics.logEvent(logName, params)
        val paramLog = params.keySet().joinToString(", ") { key -> "$key=${params.getString(key)}" }
        appLogRepository.appendAppLog("FIREBASE_EVENT", "$logName {$paramLog}")
    }
}