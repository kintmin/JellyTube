package com.kintmin.platform.running_work

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kintmin.domain.audio_edit.usecase.FetchSavingAudioMediaIdsFlowUseCase
import com.kintmin.platform.service.FileShareForegroundService
import com.kintmin.platform.worker.usecase.ExecuteAnomalyDataCheckImpl
import com.kintmin.platform.worker.usecase.ExecuteLyricsVariantCreationImpl
import com.kintmin.platform.worker.usecase.ExecuteYoutubeDownloadImpl
import com.kintmin.presentation.port.RunningWorkMonitor
import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.flow.first

class RunningWorkMonitorImpl(
    private val appContext: Context,
    private val fetchSavingAudioMediaIdsFlowUseCase: FetchSavingAudioMediaIdsFlowUseCase,
) : RunningWorkMonitor {

    override suspend fun runningWorks(): Map<RunningWork, Int> {
        val workManager = WorkManager.getInstance(appContext)
        fun List<WorkInfo>.unfinished() = count { !it.state.isFinished }

        return mapOf(
            RunningWork.YOUTUBE_DOWNLOAD to
                workManager.getWorkInfosByTagFlow(ExecuteYoutubeDownloadImpl.TAG).first().unfinished(),
            RunningWork.LYRICS_VARIANT to
                workManager.getWorkInfosByTagFlow(ExecuteLyricsVariantCreationImpl.TAG).first().unfinished(),
            RunningWork.ANOMALY_DATA_CHECK to
                workManager.getWorkInfosForUniqueWorkFlow(ExecuteAnomalyDataCheckImpl.WORK_NAME).first().unfinished(),
            RunningWork.AUDIO_EDIT_SAVE to fetchSavingAudioMediaIdsFlowUseCase().first().size,
            RunningWork.PC_FILE_SHARE to if (FileShareForegroundService.isPcFileShareRunning.value) 1 else 0,
        ).filterValues { it > 0 }
    }
}
