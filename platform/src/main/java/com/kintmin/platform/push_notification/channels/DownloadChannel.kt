package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object DownloadChannel : PushNotificationChannel {
    override val id = "download_channel"
    override val name = "음원 다운로드 현황"
    override val description = "음원 다운로드 현황을 보여주는 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_LOW
}