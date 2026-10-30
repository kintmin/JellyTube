package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.channels.AudioEditSaveChannel

/**
 * 편집한 음원을 렌더하는 동안 띄우는 진행 알림. 저장 Worker의 포그라운드 알림으로도 쓴다.
 *
 * 여러 음원을 동시에 저장할 수 있어 저장마다 다른 [id]를 쓴다. 한 저장 안에서는 띄울 때와 지울 때
 * 같은 알림이어야 하므로, Worker가 한 번 뽑은 번호를 넘겨받는다.
 */
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
