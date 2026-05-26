package com.kintmin.platform.push_notification.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.FileShareChannel
import com.kintmin.platform.service.FileShareForegroundService

data object FileShareServerNotification : PushNotification() {

    override val id = PushNotificationIds.FILE_SHARE_SERVER
    override val channel = FileShareChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("파일 공유 받기 실행 중")
            .setContentText("PC에서 파일을 전송할 수 있습니다.")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_delete,
                "종료",
                PendingIntent.getService(
                    context,
                    com.kintmin.platform.intent.IntentRequestCode.FILE_SHARE_STOP_NOTIFICATION,
                    Intent(context, FileShareForegroundService::class.java).apply {
                        action = FileShareForegroundService.ACTION_STOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
}
