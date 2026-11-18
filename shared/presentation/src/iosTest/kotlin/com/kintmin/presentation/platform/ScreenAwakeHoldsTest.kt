package com.kintmin.presentation.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenAwakeHoldsTest {

    private val applied = mutableListOf<Boolean>()
    private val holds = ScreenAwakeHolds { applied += it }

    @Test
    fun `마지막으로 놓을 때만 풀린다`() {
        holds.acquire()
        holds.acquire()
        holds.release()
        assertEquals(listOf(true), applied)

        holds.release()
        assertEquals(listOf(true, false), applied)
    }

    @Test
    fun `붙잡지 않은 채 놓아도 남의 것을 풀지 않는다`() {
        holds.release()
        holds.acquire()
        holds.release()
        holds.release()
        holds.acquire()

        assertEquals(listOf(true, false, true), applied)
    }
}
