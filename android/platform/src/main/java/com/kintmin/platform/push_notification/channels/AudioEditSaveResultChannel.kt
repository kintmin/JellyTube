package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object AudioEditSaveResultChannel : PushNotificationChannel {
    override val id = "audio_edit_save_result_channel"
    override val name = "음원 편집 저장 결과"
    override val description = "편집한 음원 저장이 끝났을 때 받을 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_MAX
}
