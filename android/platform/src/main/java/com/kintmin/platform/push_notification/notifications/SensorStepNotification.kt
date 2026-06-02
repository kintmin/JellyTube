package com.kintmin.platform.push_notification.notifications

import android.app.Notification
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.kintmin.platform.intent.stepScreenPendingIntent
import com.kintmin.platform.push_notification.PushNotificationIds
import com.kintmin.platform.push_notification.channels.StepSensorChannel

data class SensorStepNotification(
    val stepSensorStepCount: Int,
) : PushNotification() {

    override val id = PushNotificationIds.SENSOR_STEP
    override val channel = StepSensorChannel

    override fun buildNotification(context: Context): Notification {
        val textColor = if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        val remoteViews = RemoteViews(context.packageName, com.kintmin.platform.R.layout.notification_sensor_steps).apply {
            setTextViewText(com.kintmin.platform.R.id.tv_step_sensor_value, "%,d".format(stepSensorStepCount))
            setTextColor(com.kintmin.platform.R.id.tv_step_sensor_label, textColor)
            setTextColor(com.kintmin.platform.R.id.tv_step_sensor_value, textColor)
        }

        return NotificationCompat.Builder(context, channel.id)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setGroup(channel.id)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(context.stepScreenPendingIntent())
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}