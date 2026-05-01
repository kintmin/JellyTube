package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.downloadResultPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.DownloadResultChannel

data class DownloadResultNotification(
    val contentText: String = "지금 바로 감상하기",
    val playlistId: Int? = null,
    val audioMediaId: Int? = null,
) : PushNotification() {

    override val id = PushNotificationIds.DOWNLOAD_RESULT
    override val channel = DownloadResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("음원 다운로드 결과")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                if (playlistId != null && audioMediaId != null) {
                    setContentIntent(context.downloadResultPendingIntent(playlistId, audioMediaId))
                }
            }
            .build()
}