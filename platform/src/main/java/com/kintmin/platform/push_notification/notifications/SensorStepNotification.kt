package com.kintmin.platform.push_notification.notifications

import android.app.Notification
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.StepSensorChannel

data class SensorStepNotification(
    val stepSensorStepCount: Int,
    val accelerometerStepCount: Int,
) : PushNotification() {

    override val id = PushNotificationIds.SENSOR_STEP
    override val channel = StepSensorChannel

    override fun buildNotification(context: Context): Notification {
        val remoteViews = RemoteViews(context.packageName, com.kintmin.platform.R.layout.notification_sensor_steps).apply {
            setTextViewText(com.kintmin.platform.R.id.tv_step_sensor_value, "$stepSensorStepCount")
            setTextViewText(com.kintmin.platform.R.id.tv_accelerometer_value, "$accelerometerStepCount")
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