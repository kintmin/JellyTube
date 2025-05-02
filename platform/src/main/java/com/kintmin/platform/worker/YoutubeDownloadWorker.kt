package com.kintmin.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kintmin.domain.model.Playlist
import com.kintmin.domain.usecase.DownloadYoutubeMediaUseCase
import com.kintmin.platform.mapper.toMediaItem
import com.kintmin.platform.notification.NotificationData
import com.kintmin.platform.notification.PushNotificationUtil
import com.kintmin.platform.util.MediaControllerManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class YoutubeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadYoutubeMediaUseCase: DownloadYoutubeMediaUseCase,
    private val pushNotificationUtil: PushNotificationUtil,
    private val mediaControllerManager: MediaControllerManager,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        setForegroundAsync(NotificationData.Download().getForegroundInfo(applicationContext))

        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()
        pushNotificationUtil.sendNotification(NotificationData.Download(1, 0))

        downloadYoutubeMediaUseCase(url).onSuccess { audioMedia ->
            pushNotificationUtil.sendNotification(NotificationData.Download(1, 1))
            pushNotificationUtil.cancelNotification(NotificationData.Download())
            pushNotificationUtil.sendNotification(NotificationData.DownloadResult("완료되었습니다."))
            withContext(Dispatchers.Main) {
                mediaControllerManager.tryAddLastMediaItem(Playlist.TOTAL, audioMedia.toMediaItem())
                mediaControllerManager.tryAddLastMediaItem(Playlist.UNCATEGORIZED, audioMedia.toMediaItem())
            }
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