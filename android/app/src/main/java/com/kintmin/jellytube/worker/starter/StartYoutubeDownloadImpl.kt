package com.kintmin.jellytube.worker.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.jellytube.worker.YoutubeDownloadWorker
import com.kintmin.presentation.port.StartYoutubeDownload

class StartYoutubeDownloadImpl(
    private val appContext: Context,
) : StartYoutubeDownload {

    override operator fun invoke(url: String, playlistId: Int) {
        val request = OneTimeWorkRequestBuilder<YoutubeDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(
                workDataOf(
                    YoutubeDownloadWorker.INPUT_DATA_URL to url,
                    YoutubeDownloadWorker.INPUT_DATA_PLAYLIST_ID to playlistId,
                ),
            )
            .addTag(TAG)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }

    companion object {
        const val TAG = "youtube_download"
    }
}
