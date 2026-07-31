package com.kintmin.domain.extension

import kotlinx.coroutines.CancellationException

/**
 * 코루틴 취소를 삼키지 않는 [runCatching].
 *
 * 표준 [runCatching]은 [CancellationException]까지 잡아 [Result.failure]로 바꿔버린다.
 * 그러면 이미 취소된 코루틴이 실패 분기를 타고 정상 흐름을 계속 진행해 구조적 동시성이 깨진다.
 * 취소는 그대로 상위로 던지고, 나머지 예외만 [Result.failure]로 감싼다.
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Exception) {
    Result.failure(exception)
}
