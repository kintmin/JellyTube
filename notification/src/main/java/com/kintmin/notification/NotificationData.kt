package com.kintmin.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

sealed class NotificationData {
    abstract val id: Int
    abstract val channel: NotificationChannelData

    abstract fun getNotification(context: Context): Notification?

    data class Download(
        private val maxCount: Int = 0,
        private val currentCount: Int = 0,
    ) : NotificationData() {
        override val id = 1
        override val channel = NotificationChannelData.Download

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, NotificationChannelData.Download.id)
                .setContentTitle("음원 다운로드 중...")
                .setContentText("잠시만 기다려 주세요.")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setOngoing(true)
                .setGroup(NotificationChannelData.Download.id)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(maxCount, currentCount, false)
                .build()
    }

    data class DownloadResult(
        val contentText: String = "지금 바로 감상하기"
    ) : NotificationData() {
        override val id = 2
        override val channel = NotificationChannelData.DownloadResult

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, NotificationChannelData.DownloadResult.id)
                .setContentTitle("음원 다운로드 결과")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setGroup(NotificationChannelData.DownloadResult.id)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()
    }
}
