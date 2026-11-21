package com.kintmin.log

import com.kintmin.log.model.SentryEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class SentryEventTest {

    @Test
    fun `값이 없는 파라미터는 보내지 않는다`() {
        val event = SentryEvent.FailedDownloadAudioMedia(source = "s", exception = Exception("e"))

        assertEquals(setOf("Source", "ErrorMessage"), event.attributes.keys)
    }
}
