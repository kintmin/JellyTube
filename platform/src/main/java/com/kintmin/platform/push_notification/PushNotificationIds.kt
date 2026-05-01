package com.kintmin.platform.push_notification

object PushNotificationIds {
    const val DOWNLOAD = 1
    const val DOWNLOAD_RESULT = 2
    const val SENSOR_STEP = 3
}

object PushNotificationIdGenerator {

    fun random(): Int {
        return kotlin.random.Random.nextInt(
            from = 10_000,  // PushNotificationIds 의 최대 id가 10000 미만이라고 가정
            until = Int.MAX_VALUE
        )
    }
}