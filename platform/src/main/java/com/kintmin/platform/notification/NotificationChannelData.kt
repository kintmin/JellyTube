package com.kintmin.platform.notification

import android.app.NotificationManager

sealed interface NotificationChannelData {
    val id: String
    val name: String
    val description: String
    val importance: Int

    /// 지금 재생 중 채널은 ExoPlayer가 자체 제공

    data object Download : NotificationChannelData {
        override val id = "download_channel"
        override val name = "음원 다운로드 현황"
        override val description = "음원 다운로드 현황을 보여주는 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_LOW
    }

    data object DownloadResult : NotificationChannelData {
        override val id = "download_complete_channel"
        override val name = "음원 다운로드 결과"
        override val description = "음원 다운로드가 완료되었을 때 받을 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_MAX
    }

    data object StepSensor : NotificationChannelData {
        override val id = "step_channel"
        override val name = "걸음수 채널"
        override val description = "걸음수를 측정하는 알림 채널입니다."
        override val importance = NotificationManager.IMPORTANCE_LOW
    }
}