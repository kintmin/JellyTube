package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object AudioEditSaveChannel : PushNotificationChannel {
    override val id = "audio_edit_save_channel"
    override val name = "음원 편집 저장 현황"
    override val description = "편집한 음원을 저장하는 동안 보여주는 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_LOW
}
