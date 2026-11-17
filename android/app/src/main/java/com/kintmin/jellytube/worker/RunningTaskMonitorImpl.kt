package com.kintmin.jellytube.worker

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kintmin.domain.audio_edit.usecase.FetchSavingAudioMediaIdsFlowUseCase
import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.jellytube.worker.usecase.StartAnomalyDataCheckImpl
import com.kintmin.jellytube.worker.usecase.StartLyricsVariantCreationImpl
import com.kintmin.jellytube.worker.usecase.StartYoutubeDownloadImpl
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.flow.first

class RunningTaskMonitorImpl(
    private val appContext: Context,
    private val fetchSavingAudioMediaIdsFlowUseCase: FetchSavingAudioMediaIdsFlowUseCase,
    private val fetchPcFileShareServerRunningUseCase: FetchPcFileShareServerRunningUseCase,
) : RunningTaskMonitor {

    override suspend fun runningTasks(): Map<RunningTask, Int> {
        val workManager = WorkManager.getInstance(appContext)
        fun List<WorkInfo>.unfinished() = count { !it.state.isFinished }

        return mapOf(
            RunningTask.YOUTUBE_DOWNLOAD to
                workManager.getWorkInfosByTagFlow(StartYoutubeDownloadImpl.TAG).first().unfinished(),
            RunningTask.LYRICS_VARIANT to
                workManager.getWorkInfosByTagFlow(StartLyricsVariantCreationImpl.TAG).first().unfinished(),
            RunningTask.ANOMALY_DATA_CHECK to
                workManager.getWorkInfosForUniqueWorkFlow(StartAnomalyDataCheckImpl.WORK_NAME).first().unfinished(),
            RunningTask.AUDIO_EDIT_SAVE to fetchSavingAudioMediaIdsFlowUseCase().first().size,
            RunningTask.PC_FILE_SHARE to if (fetchPcFileShareServerRunningUseCase().value) 1 else 0,
        ).filterValues { it > 0 }
    }
}
