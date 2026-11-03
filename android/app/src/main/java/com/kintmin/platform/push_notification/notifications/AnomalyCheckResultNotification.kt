package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.appLogPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIdGenerator
import com.kintmin.platform.push_notification.channels.AnomalyCheckResultChannel

data class AnomalyCheckResultNotification(
    val resultType: ResultType,
    val anomalyCount: Int = 0,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = AnomalyCheckResultChannel

    override fun buildNotification(context: Context): android.app.Notification {
        val contentText = resultType.contentText(anomalyCount)
        return NotificationCompat.Builder(context, channel.id)
            .setContentTitle(resultType.title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                when (resultType) {
                    ResultType.Success -> Unit
                    ResultType.Failure -> setContentIntent(context.appLogPendingIntent())
                }
            }
            .build()
    }

    enum class ResultType(val title: String) {
        Success("이상 데이터 점검 완료"),
        Failure("이상 데이터 점검 실패");

        fun contentText(anomalyCount: Int): String = when (this) {
            Success -> "이상 데이터 점검에 완료했습니다.\n발견된 이상 데이터: ${anomalyCount}건"
            Failure -> "작업 도중 오류가 발생했습니다\n앱 로그를 확인해주세요."
        }
    }
}
