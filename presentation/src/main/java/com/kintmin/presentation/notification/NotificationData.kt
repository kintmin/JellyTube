package com.kintmin.presentation.notification

import android.app.Notification
import android.content.Context

sealed interface NotificationData {
    val id: Int
    val channel: NotificationChannelData

    fun getNotification(context: Context): Notification?

    data object MusicForeground : NotificationData {
        override val id = 1
        override val channel = NotificationChannelData.Music

        // MediaSessionService 가 자동으로 생성
        override fun getNotification(context: Context) = null
    }
}
