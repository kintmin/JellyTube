package com.kintmin.presentation

import com.kintmin.presentation.port.RepeatRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/** 두 플랫폼의 `MediaControllerManager` 구현이 이것을 그대로 쓴다. */
class RepeatRangeTest {

    private val repeatRange = RepeatRange()

    @Test
    fun `누를 때마다 시작점 끝점 해제로 돈다`() {
        repeatRange.update("1", 1_000.milliseconds)
        assertEquals(1_000.milliseconds, repeatRange.stateFor("1").startDuration)
        assertNull(repeatRange.stateFor("1").endDuration)

        repeatRange.update("1", 3_000.milliseconds)
        assertEquals(3_000.milliseconds, repeatRange.stateFor("1").endDuration)

        repeatRange.update("1", 5_000.milliseconds)
        assertNull(repeatRange.stateFor("1").startDuration)
        assertNull(repeatRange.stateFor("1").endDuration)
    }

    @Test
    fun `끝점이 시작점보다 앞이면 실패로 돌려준다`() {
        repeatRange.update("1", 3_000.milliseconds)

        assertTrue(repeatRange.update("1", 1_000.milliseconds).isFailure)
        assertNull(repeatRange.stateFor("1").endDuration)
    }

    @Test
    fun `끝점이 시작점과 같아도 실패다`() {
        repeatRange.update("1", 3_000.milliseconds)

        assertTrue(repeatRange.update("1", 3_000.milliseconds).isFailure)
    }

    @Test
    fun `곡이 바뀌면 구간이 풀린다`() {
        repeatRange.update("1", 1_000.milliseconds)
        repeatRange.update("1", 3_000.milliseconds)

        val state = repeatRange.stateFor("2")

        assertNull(state.mediaId)
        assertNull(state.startDuration)
    }

    @Test
    fun `끝점을 넘으면 시작점으로 되돌린다`() {
        repeatRange.update("1", 1_000.milliseconds)
        repeatRange.update("1", 3_000.milliseconds)

        assertEquals(1_000.milliseconds, repeatRange.rewindTarget("1", 3_000.milliseconds))
        assertEquals(1_000.milliseconds, repeatRange.rewindTarget("1", 3_001.milliseconds))
        assertNull(repeatRange.rewindTarget("1", 2_999.milliseconds))
    }

    @Test
    fun `구간이 덜 잡혔으면 되돌리지 않는다`() {
        repeatRange.update("1", 1_000.milliseconds)

        assertNull(repeatRange.rewindTarget("1", 9_000.milliseconds))
    }

    @Test
    fun `잡아둔 곡이 교체되면 구간이 풀린다`() {
        repeatRange.update("1", 1_000.milliseconds)
        repeatRange.update("1", 3_000.milliseconds)

        repeatRange.clearIfHeldBy("2")
        assertEquals(1_000.milliseconds, repeatRange.stateFor("1").startDuration)

        repeatRange.clearIfHeldBy("1")
        assertNull(repeatRange.stateFor("1").startDuration)
    }
}
