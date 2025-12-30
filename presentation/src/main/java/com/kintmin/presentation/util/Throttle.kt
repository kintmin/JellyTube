package com.kintmin.presentation.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

class Throttle(private val intervalTimeMillis: Long) {

    private var lastExecuted = 0L
    private val mutex = Mutex()

    suspend operator fun invoke(action: suspend () -> Unit) {
        val calledTime = System.currentTimeMillis()
        mutex.withLock {
            if (calledTime - lastExecuted < intervalTimeMillis) return
            yield()
            action()
            lastExecuted = System.currentTimeMillis()
        }
    }
}