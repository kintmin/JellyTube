package com.kintmin.domain.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * 취소를 삼키지 않는 [runCatching].
 *
 * `runCatching` 은 [CancellationException] 까지 [Result.failure] 로 감싸서, 코루틴을 취소해도
 * 실패로 보이고 바깥 코루틴은 자기가 취소된 줄 모른 채 계속 간다. 취소는 다시 던지고 나머지만 감싼다.
 *
 * 취소 말고는 `runCatching` 과 같은 범위를 잡는다 — 이 프로젝트의 도메인 예외는 `Exception` 이
 * 아니라 `Throwable` 을 바로 상속하므로, `Exception` 만 잡으면 그것들이 실패로 잡히지 않는다.
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
