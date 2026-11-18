package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.IntentRequestCode
import com.kintmin.jellytube.intent.workResultPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIdGenerator
import com.kintmin.jellytube.push_notification.channels.TranslationResultChannel
import com.kintmin.presentation.port.model.WorkResultNotice

data class LyricsVariantResultNotification(
    val notice: WorkResultNotice.LyricsVariantCreation,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = TranslationResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                context.workResultPendingIntent(IntentRequestCode.LYRICS_VARIANT_RESULT_NOTIFICATION, notice.deepLink),
            )
            .build()
}
