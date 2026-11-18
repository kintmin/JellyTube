package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.IntentRequestCode
import com.kintmin.jellytube.intent.workResultPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIdGenerator
import com.kintmin.jellytube.push_notification.channels.AnomalyCheckResultChannel
import com.kintmin.presentation.port.model.WorkResultNotice

data class AnomalyCheckResultNotification(
    val notice: WorkResultNotice.AnomalyCheck,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = AnomalyCheckResultChannel

    override fun buildNotification(context: Context): android.app.Notification =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(context.workResultPendingIntent(IntentRequestCode.APP_LOG_NOTIFICATION, notice.deepLink))
            .build()
}
