package com.kintmin.platform.push_notification.channels

import android.app.NotificationManager

data object StepSensorChannel : PushNotificationChannel {
    override val id = "step_channel"
    override val name = "걸음수 채널"
    override val description = "걸음수를 측정하는 알림 채널입니다."
    override val importance = NotificationManager.IMPORTANCE_LOW
}