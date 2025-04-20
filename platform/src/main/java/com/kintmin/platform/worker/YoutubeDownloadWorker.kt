package com.kintmin.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.usecase.FetchYoutubeMediaUseCase
import com.kintmin.platform.notification.NotificationData
import com.kintmin.platform.notification.PushNotificationUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class YoutubeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fetchYoutubeMediaUseCase: FetchYoutubeMediaUseCase,
    private val pushNotificationUtil: PushNotificationUtil,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()

        pushNotificationUtil.sendNotification(NotificationData.Download(1, 0))

        fetchYoutubeMediaUseCase(url).onSuccess {
            pushNotificationUtil.sendNotification(NotificationData.Download(1, 1))
            pushNotificationUtil.cancelNotification(NotificationData.Download())
            pushNotificationUtil.sendNotification(NotificationData.DownloadResult("완료되었습니다."))
            return Result.success()
        }.onFailure {
            pushNotificationUtil.cancelNotification(NotificationData.Download())
            pushNotificationUtil.sendNotification(NotificationData.DownloadResult(it.message.toString()))
        }

        return Result.failure()
    }

    companion object {
        const val INPUT_DATA_URL = "youtube_url"
    }
}