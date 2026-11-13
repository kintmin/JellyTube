package com.kintmin.ios.port

import com.kintmin.presentation.work_notice.WorkResultNotice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosLocalNotificationTest {

    @Test
    fun `알림에 실은 딥링크를 탭에서 그대로 읽는다`() {
        val notice = WorkResultNotice.Download.Success(artist = "가수", name = "곡", playlistId = 1, audioMediaId = 2)

        val content = IosLocalNotification.contentOf(notice)

        assertEquals(notice.deepLink, localNotificationDeepLinkOf(content.userInfo))
    }

    @Test
    fun `딥링크가 없는 알림은 읽을 것이 없다`() {
        val content = IosLocalNotification.contentOf(WorkResultNotice.AnomalyCheck.Success(anomalyCount = 0))

        assertNull(localNotificationDeepLinkOf(content.userInfo))
    }
}
