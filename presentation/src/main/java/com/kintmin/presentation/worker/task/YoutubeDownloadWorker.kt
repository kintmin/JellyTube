package com.kintmin.presentation.worker.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.usecase.FetchYoutubeMediaUseCase
import com.kintmin.presentation.notification.NotificationData
import com.kintmin.presentation.notification.NotificationUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class YoutubeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fetchYoutubeMediaUseCase: FetchYoutubeMediaUseCase,
    private val notificationUtil: NotificationUtil,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()

        notificationUtil.sendNotification(NotificationData.Download(1, 0))

        fetchYoutubeMediaUseCase.getData(url).onSuccess {
            notificationUtil.sendNotification(NotificationData.Download(1, 1))
            notificationUtil.cancelNotification(NotificationData.Download())
            notificationUtil.sendNotification(NotificationData.DownloadResult("완료되었습니다."))
            return Result.success()
        }.onFailure {
            notificationUtil.cancelNotification(NotificationData.Download())
            notificationUtil.sendNotification(NotificationData.DownloadResult(it.message.toString()))
        }

        return Result.failure()
    }

    companion object {
        const val INPUT_DATA_URL = "youtube_url"
    }
}