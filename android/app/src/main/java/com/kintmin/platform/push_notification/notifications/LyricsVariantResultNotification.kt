package com.kintmin.platform.push_notification.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.platform.intent.appLogPendingIntent
import com.kintmin.platform.intent.lyricsViewerPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIdGenerator
import com.kintmin.platform.push_notification.channels.TranslationResultChannel

data class LyricsVariantResultNotification(
    val variant: LyricsVariant,
    val resultType: ResultType,
    val contentText: String,
    val audioMediaId: Int? = null,
) : PushNotification() {

    override val id = PushNotificationIdGenerator.random()
    override val channel = TranslationResultChannel

    override fun buildNotification(context: Context) =
        NotificationCompat.Builder(context, channel.id)
            .setContentTitle(title())
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .apply {
                when (resultType) {
                    ResultType.Success -> if (audioMediaId != null) {
                        setContentIntent(context.lyricsViewerPendingIntent(audioMediaId))
                    }

                    ResultType.Failure -> setContentIntent(context.appLogPendingIntent())
                }
            }
            .build()

    private fun title(): String {
        val label = when (variant) {
            LyricsVariant.TRANSLATION -> "가사 번역"
            LyricsVariant.TRANSLITERATION -> "가사 음차 번역"
        }
        val suffix = when (resultType) {
            ResultType.Success -> "완료"
            ResultType.Failure -> "실패"
        }
        return "$label $suffix"
    }

    enum class ResultType {
        Success,
        Failure,
    }
}
