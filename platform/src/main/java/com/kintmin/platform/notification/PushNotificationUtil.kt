package com.kintmin.platform.notification

import android.Manifest.permission
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.app.NotificationManagerCompat
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

    private fun ensureNotificationChannelExists(channelData: NotificationChannelData) {
        val channel = NotificationChannel(channelData.id, channelData.name, channelData.importance).apply {
            description = channelData.description
        }
        NotificationManagerCompat.from(appContext).createNotificationChannel(channel)
    }
}