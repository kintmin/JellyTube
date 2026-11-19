package com.kintmin.domain.audio_play_setting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerFadeOutTest {

    @Test
    fun `3초를 100밀리초로 나눈 횟수만큼 깎는다`() {
        assertEquals(30, SleepTimerFadeOut.TICK_COUNT)
    }

    @Test
    fun `페이드 시작에는 원래 볼륨이다`() {
        assertEquals(1f, SleepTimerFadeOut.volumeAt(0f), TOLERANCE)
    }

    @Test
    fun `페이드 끝에는 무음이다`() {
        assertEquals(0f, SleepTimerFadeOut.volumeAt(1f), TOLERANCE)
    }

    @Test
    fun `볼륨은 줄어들기만 한다`() {
        val volumes = (0..SleepTimerFadeOut.TICK_COUNT)
            .map { SleepTimerFadeOut.volumeAt(it.toFloat() / SleepTimerFadeOut.TICK_COUNT) }

        volumes.zipWithNext { previous, next -> assertTrue(next < previous) }
    }

    @Test
    fun `선형보다 일찍 줄어든다 — 선형은 끝에서 갑자기 사라진 것처럼 들린다`() {
        assertTrue(SleepTimerFadeOut.volumeAt(0.5f) < 0.5f)
    }

    @Test
    fun `범위를 벗어난 진행도는 양 끝으로 잡힌다`() {
        assertEquals(1f, SleepTimerFadeOut.volumeAt(-1f), TOLERANCE)
        assertEquals(0f, SleepTimerFadeOut.volumeAt(2f), TOLERANCE)
    }

    private companion object {

        const val TOLERANCE = 0.0001f
    }
}
