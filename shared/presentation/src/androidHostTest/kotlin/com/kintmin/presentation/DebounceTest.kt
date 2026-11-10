package com.kintmin.presentation

import com.kintmin.presentation.util.Debounce
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.coroutineScope
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
    fun `Debounce 기본동작 테스트`() = runTest {
        val expectedCallCount = 5
        val debounceRepeatCount = 5
        val debounce = Debounce(500L)
        var callCount = 0

        coroutineScope {
            repeat(expectedCallCount) {
                coroutineScope {
                    repeat(debounceRepeatCount) {
                        launch { debounce { ++callCount } }
                    }
                    delay(10L)
                    assertEquals(it, callCount)
                    delay(600L)
                    assertEquals(it + 1, callCount)
                }
            }
        }

        assertEquals(expectedCallCount, callCount)
    }

    @Test
    fun `비동기에서 Debounce 동작이 정상작동 해야 한다`() = runTest {
        val debounce = Debounce(500L)
        var callCount = 0

        coroutineScope {
            repeat(100_000) { _ ->
                launch {
                    debounce {
                        ++callCount
                    }
                }
            }
        }

        assertEquals(1, callCount)
    }

    @Test
    fun `Debounce의 action 에러는 부모 스코프로 전파되어야 한다`() = runTest {
        val errorMessage = "테스트"
        val debounce = Debounce(500L)

        val result = runCatching {
            coroutineScope {
                debounce {
                    throw Exception(errorMessage)
                }
            }
        }

        assertEquals(errorMessage, result.exceptionOrNull()!!.message)
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