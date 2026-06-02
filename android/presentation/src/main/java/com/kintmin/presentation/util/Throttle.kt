package com.kintmin.presentation.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Throttle(
    private val intervalTimeMillis: Long,
    private val getCurrentTime: () -> Long = { System.currentTimeMillis() },
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