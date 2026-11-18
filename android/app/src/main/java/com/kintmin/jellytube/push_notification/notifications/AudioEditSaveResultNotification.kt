package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.IntentRequestCode
import com.kintmin.jellytube.intent.workResultPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIdGenerator
import com.kintmin.jellytube.push_notification.channels.AudioEditSaveResultChannel
import com.kintmin.presentation.port.model.WorkResultNotice

data class AudioEditSaveResultNotification(
    val notice: WorkResultNotice.AudioEditSave,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = AudioEditSaveResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(
                context.workResultPendingIntent(IntentRequestCode.AUDIO_EDIT_SAVE_RESULT_NOTIFICATION, notice.deepLink),
            )
            .build()
}
