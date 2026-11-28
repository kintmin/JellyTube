package com.kintmin.presentation.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAwakeHoldsTest {

    private val holds = ScreenAwakeHolds()

    @Test
    fun `마지막으로 놓을 때만 풀린다`() {
        assertTrue(holds.acquire())
        assertFalse(holds.acquire())

        assertFalse(holds.release())
        assertTrue(holds.release())
    }

    @Test
    fun `붙잡지 않은 채 놓아도 남의 것을 풀지 않는다`() {
        assertFalse(holds.release())

        assertTrue(holds.acquire())
        assertTrue(holds.release())
        assertFalse(holds.release())

        assertTrue(holds.acquire())
    }
}
