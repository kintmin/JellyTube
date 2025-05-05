package com.kintmin.presentation.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Debounce(private val debounceTimeMillis: Long) {
    private var debounceJob: Job? = null
    private val mutex = Mutex()

    suspend operator fun invoke(
        coroutineScope: CoroutineScope,
        action: suspend () -> Unit,
    ) {
        mutex.withLock {
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(debounceTimeMillis)
                action()
            }
        }
    }
}