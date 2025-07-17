package com.kintmin.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationUtil @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun sendNotification(notificationData: NotificationData) {
        createChannelIfNotExist(notificationData)
        val notification = notificationData.getNotification(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationData.id, notification)
    }

    fun cancelNotification(notificationData: NotificationData) {
        NotificationManagerCompat.from(context).cancel(notificationData.id)
    }

    private fun createChannelIfNotExist(notificationData: NotificationData) {
        val channelData = notificationData.channel
        val channel = NotificationChannel(channelData.id, channelData.name, channelData.importance).apply {
            description = channelData.description
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}