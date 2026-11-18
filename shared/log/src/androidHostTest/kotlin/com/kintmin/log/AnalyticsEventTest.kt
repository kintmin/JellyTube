package com.kintmin.log

import com.kintmin.log.model.FirebaseEvent
import com.kintmin.log.model.analyticsUserId
import com.kintmin.log.model.toAnalyticsEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun `값이 없는 파라미터는 보내지 않는다`() {
        val event = FirebaseEvent.FailedDownloadAudioMedia(source = "s", exception = Exception("e")).toAnalyticsEvent()

        assertEquals(setOf("Source", "ErrorMessage"), event.params.keys)
    }

    @Test
    fun `값은 100자, userId 는 256자로 자른다`() {
        val event = FirebaseEvent.FailedRegisterUser(Exception("x".repeat(300))).toAnalyticsEvent()

        assertEquals(100, event.params.values.single().length)
        assertEquals(256, analyticsUserId("u".repeat(300)).length)
    }
}
