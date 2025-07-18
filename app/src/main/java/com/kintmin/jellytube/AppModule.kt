package com.kintmin.jellytube

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import com.kintmin.log.LogcatEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideLog(): Log {
        return object : Log {
            override fun sendLogcatEvent(event: LogcatEvent) {
                android.util.Log.d(event.tag, event.message)
            }

            override fun sendFirebaseEvent(event: FirebaseEvent) {
                val params = bundleOf(*event.params)

                if (BuildConfig.DEBUG) {
                    val firebaseEventTag = "JellyTubeFirebaseEvent"
                    val logMessage = if (!params.isEmpty) {
                        val formattedBundle = params.keySet().joinToString(separator = "\n\t") { key ->
                            val value = params.get(key)
                            "$key=$value(${value?.javaClass?.simpleName ?: "null"})"
                        }
                        "${event.logName}\n\t$formattedBundle"
                    } else {
                        event.logName
                    }

                    android.util.Log.d(firebaseEventTag, logMessage)
                } else {
                    // 사용자 고유값, 디바이스명, 디바이스 os,
                    //Firebase.analytics.setUserId()
                    //Firebase.analytics.setUserProperty()
                    Firebase.analytics.logEvent(event.logName, params)
                }
            }
        }
    }
}
