package com.kintmin.presentation.extension

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTimeTest {

    @Test
    fun `가사 시간을 센티초까지 표시한다`() {
        assertEquals("01:02.45", 62_450L.toLyricsTimeLabel())
        assertEquals("1:01:02.45", 3_662_450L.toLyricsTimeLabel())
        assertEquals("00:00.00", (-1L).toLyricsTimeLabel())
    }
}
