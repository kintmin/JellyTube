package com.kintmin.presentation.port

import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/** 알림 `userInfo` 에 딥링크를 싣는 키. 탭을 받는 `ios/shared` 의 `handleNotificationTap` 과 짝이다. */
const val LOCAL_NOTIFICATION_DEEP_LINK_KEY = "deepLink"

/**
 * 작업이 끝났을 때 한 번 띄우는 로컬 알림. Android 워커의 진행 알림에 해당하는 장치가
 * iOS 에는 없다. 권한이 없으면 시스템이 조용히 버린다.
 */
internal object IosLocalNotification {

    /** @param deepLink 탭했을 때 갈 화면. Android 알림의 `PendingIntent` 자리다. null 이면 앱만 연다. */
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
