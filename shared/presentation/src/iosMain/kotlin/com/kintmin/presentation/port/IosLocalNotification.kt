package com.kintmin.presentation.port

import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val DEEP_LINK_KEY = "deepLink"

/** `ios/shared` 가 알림 탭에서 넘겨받은 `userInfo` 를 읽는다. 딥링크가 없는 알림이면 null 이다. */
fun localNotificationDeepLinkOf(userInfo: Map<Any?, *>): String? = userInfo[DEEP_LINK_KEY] as? String

/**
 * 작업이 끝났을 때 한 번 띄우는 로컬 알림. Android 워커의 진행 알림에 해당하는 장치가
 * iOS 에는 없다. 권한이 없으면 시스템이 조용히 버린다.
 */
internal object IosLocalNotification {

    fun show(title: String, body: String, deepLink: String? = null) {
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NSUUID().UUIDString,
            content = contentOf(title, body, deepLink),
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    fun contentOf(title: String, body: String, deepLink: String?) = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        if (deepLink != null) {
            setUserInfo(mapOf(DEEP_LINK_KEY to deepLink))
        }
    }
}
