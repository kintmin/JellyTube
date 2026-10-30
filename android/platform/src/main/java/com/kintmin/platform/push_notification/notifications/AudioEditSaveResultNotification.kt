package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.appLogPendingIntent
import com.kintmin.platform.intent.audioMediaDetailPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIdGenerator
import com.kintmin.platform.push_notification.channels.AudioEditSaveResultChannel

/**
 * 편집한 음원 저장 결과 알림.
 *
 * 성공은 저장된 음원(덮어쓰기면 그 음원, 새 음원 저장이면 새로 생긴 음원)의 상세 화면으로,
 * 실패는 앱 로그 화면으로 연결한다.
 */
data class AudioEditSaveResultNotification(
    val resultType: ResultType,
    val contentText: String,
    val audioMediaId: Int? = null,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = AudioEditSaveResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(resultType.title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                when (resultType) {
                    ResultType.OverwriteSuccess, ResultType.NewMediaSuccess -> if (audioMediaId != null) {
                        setContentIntent(context.audioMediaDetailPendingIntent(audioMediaId))
                    }

                    ResultType.Failure -> setContentIntent(context.appLogPendingIntent())
                }
            }
            .build()

    enum class ResultType(val title: String) {
        OverwriteSuccess("편집본 저장 완료"),
        NewMediaSuccess("편집본을 새 음원으로 저장 완료"),
        Failure("편집본 저장 실패"),
    }
}
