package com.kintmin.presentation.util

import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Throttle(
    private val intervalTimeMillis: Long,
    private val getCurrentTime: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private var lastExecutedTime = -intervalTimeMillis
    private val mutex = Mutex()

    suspend operator fun invoke(action: suspend () -> Unit) {
        val calledTime = getCurrentTime()

        mutex.withLock {
            if (calledTime - lastExecutedTime < intervalTimeMillis) return
            lastExecutedTime = getCurrentTime()
        }

        action()
    }
}