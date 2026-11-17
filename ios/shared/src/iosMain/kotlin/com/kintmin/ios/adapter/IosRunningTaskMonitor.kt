package com.kintmin.ios.adapter

import com.kintmin.domain.audio_edit.usecase.FetchSavingAudioMediaIdsFlowUseCase
import com.kintmin.ios.task.IosRunningTaskTracker
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.flow.first

internal class IosRunningTaskMonitor(
    private val fetchSavingAudioMediaIdsFlowUseCase: FetchSavingAudioMediaIdsFlowUseCase,
    private val fileShareServerController: FileShareServerController,
) : RunningTaskMonitor {

    override suspend fun runningTasks(): Map<RunningTask, Int> =
        (
            IosRunningTaskTracker.snapshot() +
                (RunningTask.AUDIO_EDIT_SAVE to fetchSavingAudioMediaIdsFlowUseCase().first().size) +
                (RunningTask.PC_FILE_SHARE to if (fileShareServerController.isRunning.value) 1 else 0)
            )
            .filterValues { it > 0 }
}
