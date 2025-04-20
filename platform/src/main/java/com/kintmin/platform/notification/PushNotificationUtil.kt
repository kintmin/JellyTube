package com.kintmin.platform.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sendNotification(notificationData: NotificationData) {
        notificationData.channel.createChannelIfNotExist(context)
        val notification = notificationData.getNotification(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationData.id, notification)
    }

    fun cancelNotification(notificationData: NotificationData) {
        NotificationManagerCompat.from(context).cancel(notificationData.id)
    }
}