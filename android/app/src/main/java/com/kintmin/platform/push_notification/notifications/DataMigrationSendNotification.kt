package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.FileShareChannel

/** 이전은 보내기 화면이 연다. 알림에서 끄면 화면이 모르는 새 서버가 내려가므로 끄는 동작을 두지 않는다. */
data object DataMigrationSendNotification : PushNotification() {

    override val id = PushNotificationIds.FILE_SHARE_SERVER
    override val channel = FileShareChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("데이터 이전 보내기 실행 중")
            .setContentText("보내기 화면을 닫으면 종료됩니다.")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
}
