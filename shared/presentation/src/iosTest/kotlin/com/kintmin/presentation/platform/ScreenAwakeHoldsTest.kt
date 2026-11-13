package com.kintmin.presentation.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `값이 true 인 동안만 붙잡고 다른 쪽이 붙잡은 것은 건드리지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        val running = MutableStateFlow(false)
        val job = launch { holds.holdWhile(running) }
        assertEquals(emptyList(), applied)

        running.value = true
        holds.acquire()
        running.value = false
        assertEquals(listOf(true), applied)

        holds.release()
        assertEquals(listOf(true, false), applied)
        job.cancel()
    }

    @Test
    fun `수집이 취소되면 붙잡은 것을 놓는다`() = runTest(UnconfinedTestDispatcher()) {
        val running = MutableStateFlow(true)
        val job = launch { holds.holdWhile(running) }
        assertEquals(listOf(true), applied)

        job.cancel()
        assertEquals(listOf(true, false), applied)
    }
}
