package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object AnomalyCheckResultChannel : PushNotificationChannel {
    override val id = "anomaly_check_result_channel"
    override val name = "이상 데이터 점검 결과"
    override val description = "이상 데이터 점검이 완료되었을 때 받을 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_HIGH
}
