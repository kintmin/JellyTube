package com.kintmin.platform

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kintmin.platform.worker.YoutubeDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ExecuteYoutubeDownload @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke(url: String) {
        val data = workDataOf(YoutubeDownloadWorker.INPUT_DATA_URL to url)
        val request = OneTimeWorkRequestBuilder<YoutubeDownloadWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}