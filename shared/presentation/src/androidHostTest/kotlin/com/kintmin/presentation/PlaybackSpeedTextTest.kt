package com.kintmin.presentation

import com.kintmin.presentation.ui.player_detail.dialog.toPlaybackSpeedText
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTextTest {

    @Test
    fun `toPlaybackSpeedText 는 배속이 바뀌어도 글자 수가 같다`() {
        assertEquals("x1.00", 1.0f.toPlaybackSpeedText())
        assertEquals("x1.05", 1.05f.toPlaybackSpeedText())
        assertEquals("x1.10", 1.1f.toPlaybackSpeedText())
        assertEquals("x0.25", 0.25f.toPlaybackSpeedText())
        assertEquals("x3.00", 3.0f.toPlaybackSpeedText())
    }
}
