package com.kintmin.platform.push_notification

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kintmin.platform.push_notification.notifications.PushNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationManagerImpl @Inject constructor(
    private val appContext: Context,
): PushNotificationManager {

    override fun sendNotification(notificationData: PushNotification): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(appContext, permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                    throw SecurityException("POST_NOTIFICATIONS 권한이 없습니다.")
                }
            }

            val notification = notificationData.createNotification(appContext)

            if (notificationData.channel.isChannelEnabled(appContext)) {
                NotificationManagerCompat.from(appContext).notify(notificationData.id, notification)
            } else {
                throw SecurityException("알림 채널이 꺼져있습니다: ${notificationData.channel.id}")
            }
        }
    }

    override fun cancelNotification(id: Int): Result<Unit> {
        return runCatching {
            NotificationManagerCompat.from(appContext).cancel(id)
        }
    }
}