package com.kintmin.jellytube.push_notification

import com.kintmin.jellytube.push_notification.notifications.PushNotification

interface PushNotificationManager {

    fun sendNotification(notificationData: PushNotification): Result<Unit>
    fun cancelNotification(id: Int): Result<Unit>
}