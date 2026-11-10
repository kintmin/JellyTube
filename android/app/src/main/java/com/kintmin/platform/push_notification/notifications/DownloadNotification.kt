package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.DownloadChannel

data class DownloadNotification(
    private val maxCount: Int = 0,
    private val currentCount: Int = 0,
) : PushNotification() {

    override val id = PushNotificationIds.DOWNLOAD
    override val channel = DownloadChannel

    override fun buildNotification(context: Context) =
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