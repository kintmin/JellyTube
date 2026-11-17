package com.kintmin.ios.task

import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * iOS 작업은 앱 프로세스의 코루틴으로 돈다. 작업을 거는 자리에서 시작과 끝을 센다.
 *
 * Android 는 같은 수를 WorkManager 에 물어본다. iOS 에 그런 기구가 없어 직접 센다 —
 * 어댑터가 아니라 그 수를 들고 있는 자리라 `adapter/` 밖에 둔다.
 */
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
