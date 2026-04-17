package com.kintmin.platform.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat

sealed interface NotificationData {

    val id: Int
    val channel: NotificationChannelData
    fun getNotification(context: Context): Notification

    data class Download(
        private val maxCount: Int = 0,
        private val currentCount: Int = 0,
    ) : NotificationData {

        override val id = 1
        override val channel = NotificationChannelData.Download

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, channel.id)
                .setContentTitle("음원 다운로드 중...")
                .setContentText("잠시만 기다려 주세요.")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setGroup(channel.id)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(maxCount, currentCount, true)
                .build()
    }

    data class DownloadResult(
        val contentText: String = "지금 바로 감상하기"
    ) : NotificationData {

        override val id = 2
        override val channel = NotificationChannelData.DownloadResult

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, channel.id)
                .setContentTitle("음원 다운로드 결과")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setGroup(channel.id)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
    }
}
