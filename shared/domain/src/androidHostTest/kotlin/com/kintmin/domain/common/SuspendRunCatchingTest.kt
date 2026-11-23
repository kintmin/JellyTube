package com.kintmin.domain.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SuspendRunCatchingTest {

    @Test
    fun 성공하면_값을_담아_돌려준다() = runTest {
        val result = suspendRunCatching { 7 }

        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrNull())
    }

    @Test
    fun 예외는_실패로_감싼다() = runTest {
        val result = suspendRunCatching { throw IllegalStateException("터짐") }

        assertTrue(result.isFailure)
        assertEquals("터짐", result.exceptionOrNull()?.message)
    }

    /** 도메인 예외가 Throwable 을 바로 상속한다. Exception 만 잡으면 실패로 잡히지 않는다. */
    @Test
    fun Exception_이_아닌_Throwable_도_실패로_감싼다() = runTest {
        val result = suspendRunCatching { throw object : Throwable("예외가 아닌 것") {} }

        assertTrue(result.isFailure)
        assertEquals("예외가 아닌 것", result.exceptionOrNull()?.message)
    }

    @Test
    fun 취소는_감싸지_않고_다시_던진다() = runTest {
        var caught: Throwable? = null

        try {
            suspendRunCatching { throw CancellationException("멈춤") }
        } catch (e: CancellationException) {
            caught = e
        }

        assertEquals("멈춤", caught?.message)
    }

    /** runCatching 이었다면 취소가 Result 로 잡혀 블록 다음 줄이 이어서 실행된다. */
    @Test
    fun 취소된_뒤에는_블록_다음_줄이_실행되지_않는다() = runTest {
        val started = CompletableDeferred<Unit>()
        var continuedAfterCancel = false

        val job = launch {
            suspendRunCatching {
                started.complete(Unit)
                awaitCancellation()
            }
            continuedAfterCancel = true
        }
        started.await()
        job.cancelAndJoin()

        assertFalse(continuedAfterCancel)
    }
}
