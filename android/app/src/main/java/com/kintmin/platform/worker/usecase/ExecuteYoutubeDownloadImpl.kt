package com.kintmin.platform.worker.usecase

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.platform.worker.YoutubeDownloadWorker
import com.kintmin.presentation.port.ExecuteYoutubeDownload

class ExecuteYoutubeDownloadImpl(
    private val appContext: Context,
) : ExecuteYoutubeDownload {

    override operator fun invoke(url: String) {
        val request = OneTimeWorkRequestBuilder<YoutubeDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(YoutubeDownloadWorker.INPUT_DATA_URL to url))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(appContext).enqueue(request)
    }

    companion object {
        const val TAG = "youtube_download"
    }
}
