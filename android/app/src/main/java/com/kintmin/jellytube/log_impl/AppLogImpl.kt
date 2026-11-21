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
import com.kintmin.log.model.SentryEvent

class AppLogImpl(
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
        Firebase.analytics.setUserId(userId)
        appLogRepository.appendAppLog("FIREBASE_CONFIG", "setUserId=$userId")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        Log.d(debugLog.tag, debugLog.message)
        appLogRepository.appendAppLog("DEBUG", "${debugLog.tag}: ${debugLog.message}")
    }

    override fun sendSentryEvent(sentryEvent: SentryEvent) {
        val logName = sentryEvent.logName.name
        val params = bundleOf(*sentryEvent.attributes.toList().toTypedArray())

        if (BuildConfig.DEBUG) {
            val logMessage = if (!params.isEmpty) {
                val formattedBundle = params.keySet().joinToString(separator = "\n\t") { key ->
                    "$key=${params.getString(key)}"
                }
                "${logName}\n\t$formattedBundle"
            } else {
                logName
            }

            Log.d("JellyTubeSentryEvent", logMessage)
        }

        Firebase.analytics.logEvent(logName, params)
        val paramLog = params.keySet().joinToString(", ") { key -> "$key=${params.getString(key)}" }
        appLogRepository.appendAppLog("FIREBASE_EVENT", "$logName {$paramLog}")
    }
}