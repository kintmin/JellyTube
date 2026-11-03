package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.appLogPendingIntent
import com.kintmin.platform.intent.downloadResultPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIdGenerator
import com.kintmin.platform.push_notification.channels.DownloadResultChannel

data class DownloadResultNotification(
    val resultType: ResultType,
    val contentText: String,
    val playlistId: Int? = null,
    val audioMediaId: Int? = null,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = DownloadResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(resultType.title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                when (resultType) {
                    ResultType.Success -> {
                        if (playlistId != null && audioMediaId != null) {
                            setContentIntent(context.downloadResultPendingIntent(playlistId, audioMediaId))
                        }
                    }
                    ResultType.Failure -> setContentIntent(context.appLogPendingIntent())
                }
            }
            .build()

    enum class ResultType(val title: String) {
        Success("음원 다운로드 완료"),
        Failure("음원 다운로드 실패"),
    }
}