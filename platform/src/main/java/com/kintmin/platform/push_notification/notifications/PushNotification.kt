package com.kintmin.platform.push_notification.notifications

import android.app.Notification
import android.content.Context
import com.kintmin.platform.push_notification.channels.PushNotificationChannel

sealed class PushNotification {

    abstract val id: Int
    abstract val channel: PushNotificationChannel
    protected abstract fun buildNotification(context: Context): Notification

    fun createNotification(context: Context): Notification {
        channel.ensureNotificationChannelExists(context)
        return buildNotification(context)
    }
}