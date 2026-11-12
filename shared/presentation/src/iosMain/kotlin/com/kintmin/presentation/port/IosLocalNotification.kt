package com.kintmin.presentation.port

import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/** 탭을 받는 `ios/shared` 가 읽어야 해서 internal 로 내리지 않는다. */
const val LOCAL_NOTIFICATION_DEEP_LINK_KEY = "deepLink"

/**
 * 작업이 끝났을 때 한 번 띄우는 로컬 알림. Android 워커의 진행 알림에 해당하는 장치가
 * iOS 에는 없다. 권한이 없으면 시스템이 조용히 버린다.
 */
internal object IosLocalNotification {

    fun show(title: String, body: String, deepLink: String? = null) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            if (deepLink != null) {
                setUserInfo(mapOf(LOCAL_NOTIFICATION_DEEP_LINK_KEY to deepLink))
            }
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
