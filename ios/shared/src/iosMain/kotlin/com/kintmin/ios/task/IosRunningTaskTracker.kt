package com.kintmin.ios.task

import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** iOS 작업은 앱 프로세스의 코루틴으로 돈다. 작업을 거는 자리에서 시작과 끝을 센다. */
internal object IosRunningTaskTracker {

    private val counts = MutableStateFlow<Map<RunningTask, Int>>(emptyMap())

    suspend fun <T> track(task: RunningTask, block: suspend () -> T): T {
        counts.update { it + (task to (it[task] ?: 0) + 1) }
        try {
            return block()
        } finally {
            counts.update { it + (task to (it[task] ?: 1) - 1) }
        }
    }

    fun snapshot(): Map<RunningTask, Int> = counts.value
}
