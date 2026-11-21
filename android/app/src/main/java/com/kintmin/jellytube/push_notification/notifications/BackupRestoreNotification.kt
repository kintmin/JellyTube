package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.bringAppToFrontPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIds
import com.kintmin.jellytube.push_notification.channels.FileShareChannel

data object BackupRestoreNotification : PushNotification() {

    override val id = PushNotificationIds.FILE_SHARE_SERVER
    override val channel = FileShareChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("백업 복원 중")
            .setContentText("백업에 있던 곡을 다시 받고 있습니다.")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(context.bringAppToFrontPendingIntent())
            .build()
}
