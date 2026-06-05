package com.kintmin.presentation

import com.kintmin.presentation.util.Throttle
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottleTest {

    @Test
    fun `Throttle 기본동작 테스트`() = runTest {
        val expectedCallCount = 5
        val throttleRepeatCount = 5
        val throttle = Throttle(500L) {
            testScheduler.currentTime
        }
        var callCount = 0

        coroutineScope {
            repeat(expectedCallCount) { index ->
                coroutineScope {
                    repeat(throttleRepeatCount) {
                        launch { throttle { ++callCount } }
                    }
                    delay(10L)
                    assertEquals(index + 1, callCount)
                    delay(600L)
                    assertEquals(index + 1, callCount)
                }
            }
        }


    }

    @Test
    fun `비동기에서 Throttle 동작이 정상작동 해야 한다`() = runTest {
        val throttle = Throttle(500L) {
            testScheduler.currentTime
        }
        var callCount = 0

        coroutineScope {
            repeat(100_000) { _ ->
                launch {
                    throttle {
                        ++callCount
                    }
                }
            }
        }

        assertEquals(1, callCount)
    }

    @Test
    fun `Throttle의 action 에러는 전파되어야 한다`() = runTest {
        val errorMessage = "테스트"
        val throttle = Throttle(500L)

        val result = runCatching {
            throttle {
                throw Exception(errorMessage)
            }
        }

        assertEquals(errorMessage, result.exceptionOrNull()!!.message)
    }

    @Test
    fun `Throttle은 구조화된 동시성이 지켜져야 한다`() = runTest {
        val throttle = Throttle(500L) {
            testScheduler.currentTime
        }
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
        val throttle = Throttle(500L) {
            testScheduler.currentTime
        }
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