package com.kintmin.platform.notification

import android.app.Notification
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.downloadResultPendingIntent
import com.kintmin.platform.R

sealed interface NotificationData {

    val id: Int
    val channel: NotificationChannelData
    fun getNotification(context: Context): Notification

    data class Download(
        private val maxCount: Int = 0,
        private val currentCount: Int = 0,
    ) : NotificationData {

        override val id = 1
        override val channel = NotificationChannelData.Download

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, channel.id)
                .setContentTitle("음원 다운로드 중...")
                .setContentText("잠시만 기다려 주세요.")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setGroup(channel.id)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(maxCount, currentCount, true)
                .build()
    }

    data class DownloadResult(
        val contentText: String = "지금 바로 감상하기",
        val playlistId: Int? = null,
        val audioMediaId: Int? = null,
    ) : NotificationData {

        override val id = 2
        override val channel = NotificationChannelData.DownloadResult

        override fun getNotification(context: Context) =
            NotificationCompat.Builder(context, channel.id)
                .setContentTitle("음원 다운로드 결과")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setGroup(channel.id)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .apply {
                    if (playlistId != null && audioMediaId != null) {
                        setContentIntent(context.downloadResultPendingIntent(playlistId, audioMediaId))
                    }
                }
                .build()
    }

    data class SensorStep(
        val stepSensorStepCount: Int,
        val accelerometerStepCount: Int,
    ) : NotificationData {

        override val id = 3
        override val channel = NotificationChannelData.StepSensor

        override fun getNotification(context: Context): Notification {
            val remoteViews = RemoteViews(context.packageName, R.layout.notification_sensor_steps).apply {
                setTextViewText(R.id.tv_step_sensor_value, "$stepSensorStepCount")
                setTextViewText(R.id.tv_accelerometer_value, "$accelerometerStepCount")
            }

            return NotificationCompat.Builder(context, channel.id)
                .setCustomContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setGroup(channel.id)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
        }
    }
}
