package com.kintmin.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

sealed class NotificationChannelData {
    abstract val id: String
    abstract val name: String
    abstract val description: String
    abstract val importance: Int

    fun createChannelIfNotExist(context: Context) {
        val channel = NotificationChannel(id, name, importance).apply {
            description =  this@NotificationChannelData.description
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    data object NowPlaying : NotificationChannelData() {
        override val id = "now_playing_channel"
        override val name = "지금 재생 중"
        override val description = "현재 재생중인 음원을 보여주는 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_LOW
    }

    data object Download : NotificationChannelData() {
        override val id = "download_channel"
        override val name = "음원 다운로드 현황"
        override val description = "음원 다운로드 현황을 보여주는 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_LOW
    }

    data object DownloadResult : NotificationChannelData() {
        override val id = "download_complete_channel"
        override val name = "음원 다운로드 결과"
        override val description = "음원 다운로드가 완료되었을 때 받을 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_MAX
    }
}