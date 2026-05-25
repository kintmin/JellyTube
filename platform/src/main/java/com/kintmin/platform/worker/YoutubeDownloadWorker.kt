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
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadNotification
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.mapper.toMediaControlData
import com.kintmin.platform.service_controller.model.MediaControlData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class YoutubeDownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase,
    private val mediaControllerManager: MediaControllerManager,
    private val pushNotificationManager: PushNotificationManager,
    private val appLog: AppLog,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationData = DownloadNotification()
        val notification = notificationData.createNotification(appContext)
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
            val errorMessage = "포그라운드 서비스 시작 실패: ${exception?.message ?: "알 수 없는 오류"}"

            pushNotificationManager.sendNotification(
                DownloadResultNotification(
                    resultType = DownloadResultNotification.ResultType.Failure,
                    contentText = errorMessage,
                )
            )
            appLog.sendDebugLog(DebugLog("YoutubeDownloadWorker", errorMessage))
            return Result.failure()
        }

        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()
        val downloadNotification = DownloadNotification(1, 0)
        pushNotificationManager.sendNotification(downloadNotification)

        downloadAudioMediaUseCase(url).onSuccess { result ->
            pushNotificationManager.cancelNotification(downloadNotification.id)
            pushNotificationManager.sendNotification(
                DownloadResultNotification(
                    resultType = DownloadResultNotification.ResultType.Success,
                    contentText = "${result.audioMedia.artist} - ${result.audioMedia.name}",
                    playlistId = result.playlistIdOnDownload,
                    audioMediaId = result.audioMedia.id,
                )
            )
            withContext(Dispatchers.Main) {
                val mediaControlData = result.audioMedia.toMediaControlData()
                tryAddMediaItem(
                    playlistId = Playlist.TOTAL,
                    mediaControlData = mediaControlData,
                    shouldInsertAtTop = result.shouldInsertAtTopOnDownload,
                )

                if (result.playlistIdOnDownload != Playlist.TOTAL) {
                    tryAddMediaItem(
                        playlistId = result.playlistIdOnDownload,
                        mediaControlData = mediaControlData,
                        shouldInsertAtTop = result.shouldInsertAtTopOnDownload,
                    )
                }
            }
            return Result.success()
        }.onFailure {
            pushNotificationManager.cancelNotification(downloadNotification.id)

            val errorMessage = it.message.toString()
            pushNotificationManager.sendNotification(
                DownloadResultNotification(
                    resultType = DownloadResultNotification.ResultType.Failure,
                    contentText = "다운에 실패했습니다. 로그를 확인해주세요.",
                )
            )
            appLog.sendDebugLog(DebugLog("YoutubeDownloadWorker", errorMessage))
        }

        return Result.failure()
    }

    companion object {
        const val INPUT_DATA_URL = "youtube_url"
    }

    private fun tryAddMediaItem(
        playlistId: Int,
        mediaControlData: MediaControlData,
        shouldInsertAtTop: Boolean,
    ) {
        if (shouldInsertAtTop) {
            mediaControllerManager.tryAddFirstMediaItem(playlistId, mediaControlData)
        } else {
            mediaControllerManager.tryAddLastMediaItem(playlistId, mediaControlData)
        }
    }
}
