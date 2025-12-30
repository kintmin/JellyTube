package com.kintmin.presentation.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Throttle(private val intervalTimeMillis: Long) {

    private var lastExecuted = 0L
    private val mutex = Mutex()

    suspend operator fun invoke(action: suspend () -> Unit) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastExecuted < intervalTimeMillis) return
            lastExecuted = now
        }
        action()
    }
}