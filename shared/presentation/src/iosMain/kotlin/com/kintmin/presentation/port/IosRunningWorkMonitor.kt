package com.kintmin.presentation.port

import com.kintmin.domain.audio_edit.usecase.FetchSavingAudioMediaIdsFlowUseCase
import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/** iOS 작업은 앱 프로세스의 코루틴으로 돈다. 작업을 거는 자리에서 시작과 끝을 센다. */
internal object IosRunningWorkTracker {

    private val counts = MutableStateFlow<Map<RunningWork, Int>>(emptyMap())

    suspend fun <T> track(work: RunningWork, block: suspend () -> T): T {
        counts.update { it + (work to (it[work] ?: 0) + 1) }
        try {
            return block()
        } finally {
            counts.update { it + (work to (it[work] ?: 1) - 1) }
        }
    }

    fun snapshot(): Map<RunningWork, Int> = counts.value
}

internal class IosRunningWorkMonitor(
    private val fetchSavingAudioMediaIdsFlowUseCase: FetchSavingAudioMediaIdsFlowUseCase,
    private val fileShareServerController: FileShareServerController,
) : RunningWorkMonitor {

    override suspend fun runningWorks(): Map<RunningWork, Int> =
        (
            IosRunningWorkTracker.snapshot() +
                (RunningWork.AUDIO_EDIT_SAVE to fetchSavingAudioMediaIdsFlowUseCase().first().size) +
                (RunningWork.PC_FILE_SHARE to if (fileShareServerController.isRunning.value) 1 else 0)
            )
            .filterValues { it > 0 }
}
