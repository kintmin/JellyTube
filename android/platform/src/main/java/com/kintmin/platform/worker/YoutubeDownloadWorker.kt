package com.kintmin.platform.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadNotification
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.mapper.toMediaControlData
import com.kintmin.platform.service_controller.model.MediaControlData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class YoutubeDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase by inject()
    private val mediaControllerManager: MediaControllerManager by inject()
    private val pushNotificationManager: PushNotificationManager by inject()
    private val appLog: AppLog by inject()

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
                    playlistId = result.totalPlaylistId,
                    mediaControlData = mediaControlData,
                    shouldInsertAtTop = result.shouldInsertAtTopOnDownload,
                )

                if (result.playlistIdOnDownload != result.totalPlaylistId) {
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
