package com.kintmin.domain.audio_media

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParallelDownloaderTest {

    @Test
    fun `동시에 도는 작업은 상한을 넘지 않는다`() = runTest {
        var running = 0
        var maxRunning = 0

        ParallelDownloader.run((1..10).toList(), concurrency = 3) {
            running++
            maxRunning = maxOf(maxRunning, running)
            delay(100)
            running--
        }

        assertEquals(3, maxRunning)
    }

    @Test
    fun `실패한 항목은 한 번 더 시도하고 결과는 입력 순서를 지킨다`() = runTest {
        val attempts = mutableMapOf<Int, Int>()
        val done = mutableListOf<Int>()

        val results = ParallelDownloader.run(listOf(1, 2, 3), onDone = { done += it }) { item ->
            val attempt = (attempts[item] ?: 0) + 1
            attempts[item] = attempt
            when {
                item == 2 -> error("늘 실패")
                item == 3 && attempt == 1 -> error("처음만 실패")
                else -> item * 10
            }
        }

        assertEquals(listOf(10, null, 30), results.map { it.getOrNull() })
        assertEquals(mapOf(1 to 1, 2 to 2, 3 to 2), attempts)
        assertEquals(listOf(1, 2, 3), done)
    }

    @Test
    fun `취소하면 남은 항목을 시작하지 않는다`() = runTest {
        val started = mutableListOf<Int>()
        val job = launch {
            ParallelDownloader.run((1..10).toList(), concurrency = 2) {
                started += it
                delay(1_000)
            }
        }
        runCurrent()
        advanceTimeBy(500)
        job.cancel()
        yield()

        assertTrue(job.isCancelled)
        assertEquals(listOf(1, 2), started)
    }
}
