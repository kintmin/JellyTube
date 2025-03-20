package com.kintmin.presentation.notification

import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun sendNotification(notificationData: NotificationData) {
        notificationData.channel.createChannelIfNotExist(context)
        val notification = notificationData.getNotification(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationData.id, notification)
    }
}