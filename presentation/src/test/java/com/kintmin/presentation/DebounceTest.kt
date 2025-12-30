package com.kintmin.presentation

import com.kintmin.presentation.util.Debounce
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class DebounceTest {

    @Test
    fun `Debounce 동작이 정상작동 해야 한다`() = runTest {
        val debounce = Debounce(500L)
        var callCount = 0

        repeat(100_000) { _ ->
            launch {
                debounce { ++callCount }
            }
        }

        delay(400L)
        assertEquals(0, callCount)
        delay(200L)
        assertEquals(1, callCount)
    }

    @Test
    fun `Debounce는 구조화된 동시성이 지켜져야 한다`() = runTest {
        val debounce = Debounce(500L)
        var context: CoroutineContext? = null
        val name = CoroutineName("테스트 Context 이름")

        withContext(name) {
            debounce {
                context = currentCoroutineContext()
            }
            delay(1000L)
        }

        assertEquals(name, context?.get(CoroutineName))
    }

    @Test
    fun `Debounce는 부모 context의 취소 전파를 받아야 한다`() = runTest {
        val debounce = Debounce(500L)
        var called = false
        val parentJob = launch { debounce { called = true } }

        delay(100)
        parentJob.cancel()

        delay(1000)
        assertEquals(false, called)
    }
}