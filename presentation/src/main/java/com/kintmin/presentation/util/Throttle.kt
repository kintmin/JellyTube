package com.kintmin.presentation.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class Throttle(private val intervalTimeMillis: Long) {
    private val lastExecuted = AtomicLong(0L)
    private val mutex = Mutex()

    suspend operator fun invoke(action: suspend () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastExecuted.get() < intervalTimeMillis) return

        mutex.withLock {
            val updatedNow = System.currentTimeMillis()
            if (updatedNow - lastExecuted.get() >= intervalTimeMillis) {
                lastExecuted.set(updatedNow)
                action()
            }
        }
    }
}