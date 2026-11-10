package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object FileShareChannel : PushNotificationChannel {
    override val id = "file_share_channel"
    override val name = "파일 공유 받기"
    override val description = "PC에서 음원 파일을 공유 받을 때 표시되는 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_LOW
}
