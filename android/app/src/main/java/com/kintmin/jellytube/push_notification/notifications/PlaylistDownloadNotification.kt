package com.kintmin.jellytube.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.jellytube.intent.bringAppToFrontPendingIntent
import com.kintmin.jellytube.push_notification.PushNotificationIds
import com.kintmin.jellytube.push_notification.channels.FileShareChannel

data object PlaylistDownloadNotification : PushNotification() {

    override val id = PushNotificationIds.FILE_SHARE_SERVER
    override val channel = FileShareChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("재생목록 받는 중")
            .setContentText("유튜브 재생목록의 곡을 받고 있습니다.")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(context.bringAppToFrontPendingIntent())
            .build()
}
