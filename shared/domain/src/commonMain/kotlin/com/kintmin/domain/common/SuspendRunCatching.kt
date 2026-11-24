package com.kintmin.domain.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * 취소를 삼키지 않는 [runCatching].
 *
 * `runCatching` 은 [CancellationException] 까지 [Result.failure] 로 감싸서, 코루틴을 취소해도
 * 실패로 보이고 바깥 코루틴은 자기가 취소된 줄 모른 채 계속 간다. 취소는 다시 던지고 나머지만 감싼다.
 *
 * `runCatching` 과 달리 `Error` 는 잡지 않는다. OutOfMemoryError 나 NoClassDefFoundError 를
 * `Result.failure` 로 바꾸면 앱이 망가진 상태로 계속 가고, 수집기에도 평범한 실패로 올라간다.
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Exception) {
    Result.failure(exception)
}
