package com.kintmin.platform.push_notification.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.IntentRequestCode
import com.kintmin.platform.intent.bringAppToFrontPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.FileShareChannel
import com.kintmin.platform.service.FileShareForegroundService

/** 이전은 보내기 화면이 연다. 알림에서 끄면 화면이 모르는 새 서버가 내려가므로 끄는 동작을 두지 않는다. */
data object DataMigrationSendNotification : PushNotification() {

    override val id = PushNotificationIds.FILE_SHARE_SERVER
    override val channel = FileShareChannel

    // Android 14 부터는 ongoing 이어도 사용자가 지울 수 있다. 지워지면 서비스가 다시 띄운다.
    private fun repostPendingIntent(context: Context): PendingIntent = PendingIntent.getService(
        context,
        IntentRequestCode.DATA_MIGRATION_SEND_NOTIFICATION_DELETED,
        Intent(context, FileShareForegroundService::class.java).apply {
            action = FileShareForegroundService.ACTION_REPOST_NOTIFICATION
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("데이터 이전 보내기 실행 중")
            .setContentText("보내기 화면을 닫으면 종료됩니다.")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(context.bringAppToFrontPendingIntent())
            .setDeleteIntent(repostPendingIntent(context))
            .build()
}
