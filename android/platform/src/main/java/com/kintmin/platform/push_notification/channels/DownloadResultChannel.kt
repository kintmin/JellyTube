package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object DownloadResultChannel : PushNotificationChannel {
    override val id = "download_complete_channel"
    override val name = "음원 다운로드 결과"
    override val description = "음원 다운로드가 완료되었을 때 받을 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_MAX
}