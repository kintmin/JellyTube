package com.kintmin.platform.push_notification.channels

import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationManagerCompat

// [지금 재생 중] 채널은 ExoPlayer가 자체 제공
sealed interface PushNotificationChannel {

    val id: String
    val name: String
    val importance: Int
    val description: String

    fun ensureNotificationChannelExists(context: Context) {
        val channel = NotificationChannel(id, name, importance).apply {
            description = this@PushNotificationChannel.description
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun isChannelEnabled(context: Context): Boolean {
        val channel = NotificationManagerCompat.from(context).getNotificationChannel(id)
        return channel?.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }
}