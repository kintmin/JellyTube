package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.IntentRequestCode
import com.kintmin.jellytube.intent.workResultPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIdGenerator
import com.kintmin.jellytube.push_notification.channels.DownloadResultChannel
import com.kintmin.presentation.port.model.WorkResultNotice

data class DownloadResultNotification(
    val notice: WorkResultNotice.Download,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = DownloadResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                context.workResultPendingIntent(IntentRequestCode.DOWNLOAD_RESULT_NOTIFICATION, notice.deepLink),
            )
            .build()
}
