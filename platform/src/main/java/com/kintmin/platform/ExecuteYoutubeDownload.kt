package com.kintmin.platform

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.platform.worker.YoutubeDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ExecuteYoutubeDownload @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke(url: String) {
        val request = OneTimeWorkRequestBuilder<YoutubeDownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(YoutubeDownloadWorker.INPUT_DATA_URL to url))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}