package com.kintmin.presentation.notification

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

    data object Download : NotificationChannelData() {
        override val id = "download_channel"
        override val name = "음원 다운로드 현황 알림 채널"
        override val description = "음원 다운로드 현황을 보여주는 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_LOW
    }

    data object DownloadResult : NotificationChannelData() {
        override val id = "download_complete_channel"
        override val name = "음원 다운로드 결과 알림 채널"
        override val description = "음원 다운로드가 완료되었을 때 받을 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_DEFAULT
    }
}