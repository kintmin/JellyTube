package com.kintmin.presentation.port

import com.kintmin.presentation.deeplink.DeepLinkConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosLocalNotificationTest {

    @Test
    fun `알림에 실은 딥링크를 탭에서 그대로 읽는다`() {
        val deepLink = DeepLinkConstants.UrlBuilder.playlistContentScreen(playlistId = 1, audioMediaId = 2)

        val content = IosLocalNotification.contentOf("제목", "본문", deepLink)

        assertEquals(deepLink, localNotificationDeepLinkOf(content.userInfo))
    }

    @Test
    fun `딥링크 없이 띄운 알림은 읽을 것이 없다`() {
        val content = IosLocalNotification.contentOf("제목", "본문", deepLink = null)

        assertNull(localNotificationDeepLinkOf(content.userInfo))
    }
}
