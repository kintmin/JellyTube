package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object TranslationResultChannel : PushNotificationChannel {
    override val id = "lyrics_translation_result_channel"
    override val name = "가사 번역 결과"
    override val description = "가사 번역/음차 번역 생성이 완료되었을 때 받을 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_MAX
}
