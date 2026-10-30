package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.channels.AudioEditSaveChannel

class AudioEditSaveNotification(
    override val id: Int,
) : PushNotification() {

    override val channel = AudioEditSaveChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle("편집한 음원 저장 중...")
            .setContentText("잠시만 기다려 주세요.")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .build()
}
