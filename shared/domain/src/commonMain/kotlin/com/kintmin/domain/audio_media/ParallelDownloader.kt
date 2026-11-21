package com.kintmin.domain.audio_media

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ParallelDownloader {

    // 유튜브가 한 기기의 동시 요청을 제한한다. 늘리기 전에 실기기에서 429·봇 확인이 나는지 잰다.
    const val DEFAULT_CONCURRENCY = 3

    /** [onDone] 은 한 번에 하나씩 불린다. */
    suspend fun <T, R> run(
        items: List<T>,
        concurrency: Int = DEFAULT_CONCURRENCY,
        retryDelay: Duration = 2.seconds,
        onDone: (doneCount: Int) -> Unit = {},
        block: suspend (T) -> R,
    ): List<Result<R>> = coroutineScope {
        val permits = Semaphore(concurrency)
        val doneLock = Mutex()
        var doneCount = 0
        items.map { item ->
            async {
                // 다시 기다리는 동안은 자리를 내준다.
                val result = permits.withPermit { attempt(item, block) }
                    .takeIf { it.isSuccess }
                    ?: run {
                        delay(retryDelay)
                        permits.withPermit { attempt(item, block) }
                    }
                doneLock.withLock { onDone(++doneCount) }
                result
            }
        }.awaitAll()
    }

    private suspend fun <T, R> attempt(item: T, block: suspend (T) -> R): Result<R> = try {
        Result.success(block(item))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
