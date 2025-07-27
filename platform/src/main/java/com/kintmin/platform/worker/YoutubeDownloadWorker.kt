package com.kintmin.platform.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.platform.notification.NotificationData
import com.kintmin.platform.notification.PushNotificationUtil
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.platform.util.mapper.toMediaControlData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class YoutubeDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase,
    private val pushNotificationUtil: PushNotificationUtil,
    private val mediaControllerManager: MediaControllerManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationData = NotificationData.Download()
        val notification = notificationData.getNotification(appContext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationData.id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationData.id, notification)
        }
    }

    override suspend fun doWork(): Result {
        val foregroundResult = runCatching {
            setForegroundAsync(getForegroundInfo())
        }

        if (foregroundResult.isFailure) {
            val exception = foregroundResult.exceptionOrNull()
            pushNotificationUtil.sendNotification(
                NotificationData.DownloadResult("포그라운드 서비스 시작 실패: ${exception?.message ?: "알 수 없는 오류"}")
            )
            return Result.failure()
        }

        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()
        pushNotificationUtil.sendNotification(NotificationData.DownloadResult("다운로드를 시작합니다."))
        pushNotificationUtil.sendNotification(NotificationData.Download(1, 0))

        downloadAudioMediaUseCase(url).onSuccess { audioMedia ->
            pushNotificationUtil.cancelNotification(NotificationData.Download())
            pushNotificationUtil.sendNotification(NotificationData.DownloadResult("완료되었습니다."))
            withContext(Dispatchers.Main) {
                mediaControllerManager.tryAddLastMediaItem(Playlist.TOTAL, audioMedia.toMediaControlData())
                mediaControllerManager.tryAddLastMediaItem(Playlist.UNCATEGORIZED, audioMedia.toMediaControlData())
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