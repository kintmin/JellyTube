package com.kintmin.domain.audio_play_setting

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRangeTest {

    @Test
    fun `범위를 벗어난 값은 끝값으로 맞춘다`() {
        assertEquals(0.25f, PlaybackRange.clampSpeed(0.1f))
        assertEquals(3.0f, PlaybackRange.clampSpeed(4.0f))
        assertEquals(1.5f, PlaybackRange.clampSpeed(1.5f))
        assertEquals(-10, PlaybackRange.clampPitchSemitone(-24))
        assertEquals(10, PlaybackRange.clampPitchSemitone(24))
    }
}
