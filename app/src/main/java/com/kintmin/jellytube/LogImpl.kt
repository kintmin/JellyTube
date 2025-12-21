package com.kintmin.jellytube

import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import com.kintmin.log.LogcatEvent

class LogImpl : Log {
    override fun sendLogcatEvent(event: LogcatEvent) {
        android.util.Log.d(event.tag, event.message)
    }

    /**
     * 제한1: 속성 최대 개수는 25개
     * 제한2: 속성 이름 길이는 영문 기준 24자
     * 제한3: 속성값 길이는 영문 기준 36자
     * 제한4: userId 길이는 영문 기준 256자
     * https://support.google.com/analytics/answer/9267744
     */
    override fun setFirebaseConfig(rawUserId: String) {
        val userId = rawUserId.take(256)
        FirebaseCrashlytics.getInstance().setUserId(userId)
        Firebase.analytics.setUserId(userId)
    }

    override fun sendFirebaseEvent(event: FirebaseEvent) {
        val params = bundleOf(*event.params)

        if (BuildConfig.DEBUG) {
            val logMessage = if (!params.isEmpty) {
                val formattedBundle = params.keySet().joinToString(separator = "\n\t") { key ->
                    "$key=${params.getString(key)}"
                }
                "${event.logName}\n\t$formattedBundle"
            } else {
                event.logName
            }

            android.util.Log.d("JellyTubeFirebaseEvent", logMessage)
        }

        Firebase.analytics.logEvent(event.logName, params)
    }
}