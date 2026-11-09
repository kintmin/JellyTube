package com.kintmin.presentation.port

import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * 작업이 끝났을 때 띄우는 로컬 알림.
 *
 * Android 는 워커가 포그라운드 알림을 들고 돌지만 iOS 에는 그런 장치가 없다. 작업이 끝난
 * 시점에 한 번 알리는 것으로 갈음한다. 권한이 없으면 시스템이 조용히 버린다.
 */
internal object IosLocalNotification {

    fun show(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NSUUID().UUIDString,
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }
}
