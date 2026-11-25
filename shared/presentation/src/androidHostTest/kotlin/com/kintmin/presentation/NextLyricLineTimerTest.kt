package com.kintmin.presentation

import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.presentation.util.NextLyricLineTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NextLyricLineTimerTest {

    private val lines = listOf(
        LyricsLine(timeMs = 1_000L, text = "a"),
        LyricsLine(timeMs = 1_200L, text = "b"),
        LyricsLine(timeMs = 5_000L, text = "c"),
    )

    @Test
    fun `다음 줄의 시각에 그 시각을 알린다`() = runTest {
        val reached = mutableListOf<Long>()
        NextLyricLineTimer(backgroundScope).schedule(lines, 900L, 1f) { reached += it }

        advanceTimeBy(99)
        runCurrent()
        assertEquals(emptyList<Long>(), reached)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(1_000L), reached)
    }

    @Test
    fun `배속이면 그만큼 빨리 알린다`() = runTest {
        val reached = mutableListOf<Long>()
        NextLyricLineTimer(backgroundScope).schedule(lines, 800L, 2f) { reached += it }

        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf(1_000L), reached)
    }

    @Test
    fun `폴링 한 번보다 멀리 있는 줄은 예약하지 않는다`() = runTest {
        val reached = mutableListOf<Long>()
        NextLyricLineTimer(backgroundScope).schedule(lines, 1_200L, 1f) { reached += it }

        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(emptyList<Long>(), reached)
    }

    @Test
    fun `다시 예약하면 앞선 예약은 버린다`() = runTest {
        val reached = mutableListOf<Long>()
        val timer = NextLyricLineTimer(backgroundScope)
        timer.schedule(lines, 900L, 1f) { reached += it }
        timer.schedule(lines, 1_000L, 1f) { reached += it }

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(1_200L), reached)
    }

    @Test
    fun `취소하면 알리지 않는다`() = runTest {
        val reached = mutableListOf<Long>()
        val timer = NextLyricLineTimer(backgroundScope)
        timer.schedule(lines, 900L, 1f) { reached += it }
        timer.cancel()

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(emptyList<Long>(), reached)
    }
}
