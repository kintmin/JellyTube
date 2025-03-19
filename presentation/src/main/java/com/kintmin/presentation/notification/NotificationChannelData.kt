package com.kintmin.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

sealed interface NotificationChannelData {
    val id: String
    val name: String
    val importance: Int

    fun createChannelIfNotExist(context: Context) {
        val channel = NotificationChannel(id, name, importance)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    data object Music : NotificationChannelData {
        override val id = "music_channel"
        override val name = "음악 채널"
        override val importance = NotificationManager.IMPORTANCE_LOW
    }
}