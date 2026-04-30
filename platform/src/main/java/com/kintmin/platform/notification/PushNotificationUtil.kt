package com.kintmin.platform.notification

import android.Manifest.permission
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationUtil @Inject constructor(
    private val appContext: Context,
) {

    fun sendNotification(notificationData: NotificationData) {
        if (checkSelfPermission(appContext, permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) return

        ensureNotificationChannelExists(notificationData.channel)
        val notification = notificationData.getNotification(appContext)

        val notificationManager = NotificationManagerCompat.from(appContext)
        notificationManager.notify(notificationData.id, notification)
    }

    fun cancelNotification(notificationData: NotificationData) {
        NotificationManagerCompat.from(appContext).cancel(notificationData.id)
    }

    /**
     * [권한허용, 알림허용, 채널허용] 중 하나라도 안되어 있으면 Exception 발생
     */
    fun startForeground(service: Service, notificationData: NotificationData): Result<Unit> {
        return runCatching {
            ensureNotificationChannelExists(notificationData.channel)
            val notification = notificationData.getNotification(appContext)

            ServiceCompat.startForeground(
                service,
                notificationData.id,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    0
                },
            )
        }
    }

    private fun ensureNotificationChannelExists(channelData: NotificationChannelData) {
        val channel = NotificationChannel(channelData.id, channelData.name, channelData.importance).apply {
            description = channelData.description
        }
        NotificationManagerCompat.from(appContext).createNotificationChannel(channel)
    }
}