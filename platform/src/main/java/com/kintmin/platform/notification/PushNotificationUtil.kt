package com.kintmin.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationUtil @Inject constructor(
    private val appContext: Context,
) {
    fun sendNotification(notificationData: NotificationData) {
        createChannelIfNotExist(notificationData)
        val notification = notificationData.getNotification(appContext)
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationData.id, notification)
    }

    fun cancelNotification(notificationData: NotificationData) {
        NotificationManagerCompat.from(appContext).cancel(notificationData.id)
    }

    private fun createChannelIfNotExist(notificationData: NotificationData) {
        val channelData = notificationData.channel
        val channel = NotificationChannel(channelData.id, channelData.name, channelData.importance).apply {
            description = channelData.description
        }
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}