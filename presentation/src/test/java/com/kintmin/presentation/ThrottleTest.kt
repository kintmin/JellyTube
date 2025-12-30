package com.kintmin.presentation

import com.kintmin.presentation.util.Throttle
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class ThrottleTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Throttle 동작이 정상작동 해야 한다`() = runTest {
        val throttle = Throttle(100L)
        var callCount = 0

        repeat(100_000) { _ ->
            launch {
                throttle {
                    ++callCount
                }
            }
        }

        delay(250)
        assertEquals(1, callCount)
        delay(1000L)
        assertEquals(1, callCount)
    }

    @Test
    fun `Throttle은 구조화된 동시성이 지켜져야 한다`() = runTest {
        val throttle = Throttle(500L)
        var context: CoroutineContext? = null
        val name = CoroutineName("테스트 Context 이름")

        withContext(name) {
            throttle {
                context = currentCoroutineContext()
            }
            delay(1000L)
        }

        assertEquals(name, context?.get(CoroutineName))
    }

    @Test
    fun `Throttle은 부모 context의 취소 전파를 받아야 한다`() = runTest {
        val throttle = Throttle(500L)
        var called = false
        val parentJob = launch {
            throttle {
                delay(200L)
                called = true
            }
        }

        delay(100)
        parentJob.cancel()

        delay(1000)
        assertEquals(false, called)
    }
}