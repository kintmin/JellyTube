package com.kintmin.jellytube.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.jellytube.push_notification.PushNotificationManager
import com.kintmin.jellytube.push_notification.notifications.DownloadNotification
import com.kintmin.jellytube.push_notification.notifications.DownloadResultNotification
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.addDownloadedMedia
import com.kintmin.presentation.port.model.WorkResultNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.DurationUnit

class YoutubeDownloadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val downloadAudioMediaUseCase: DownloadAudioMediaUseCase by inject()
    private val downloadLyricsForAudioMediaUseCase: DownloadLyricsForAudioMediaUseCase by inject()
    private val mediaControllerManager: PlaybackController by inject()
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
        val foregroundResult = suspendRunCatching {
            setForegroundAsync(getForegroundInfo())
        }

        if (foregroundResult.isFailure) {
            pushNotificationManager.sendNotification(DownloadResultNotification(WorkResultNotice.Download.Failure))
            appLog.sendErrorLog(ErrorLog("YoutubeDownloadWorker", "포그라운드 서비스 시작 실패", foregroundResult.exceptionOrNull()))
            return Result.failure()
        }

        val url = inputData.getString(INPUT_DATA_URL) ?: return Result.failure()
        val downloadNotification = DownloadNotification(1, 0)
        pushNotificationManager.sendNotification(downloadNotification)

        downloadAudioMediaUseCase(url).onSuccess { result ->
            pushNotificationManager.cancelNotification(downloadNotification.id)
            coroutineScope {
                // 음원 등록이 이미 성공한 시점에만 시작하므로 orphan 가사/취소 시나리오는 발생하지 않는다.
                launch {
                    downloadLyricsForAudioMediaUseCase(
                        audioMediaId = result.audioMedia.id,
                        title = result.audioMedia.name,
                        targetDurationSeconds = result.audioMedia.audioDuration?.toDouble(DurationUnit.SECONDS),
                    )
                }

                pushNotificationManager.sendNotification(
                    DownloadResultNotification(
                        WorkResultNotice.Download.Success(
                            artist = result.audioMedia.artist,
                            name = result.audioMedia.name,
                            playlistId = result.playlistIdOnDownload,
                            audioMediaId = result.audioMedia.id,
                        )
                    )
                )
                withContext(Dispatchers.Main) { mediaControllerManager.addDownloadedMedia(result) }
            }
            return Result.success()
        }.onFailure {
            pushNotificationManager.cancelNotification(downloadNotification.id)

            val errorMessage = it.message.toString()
            pushNotificationManager.sendNotification(DownloadResultNotification(WorkResultNotice.Download.Failure))
            appLog.sendDebugLog(DebugLog("YoutubeDownloadWorker", errorMessage))
        }

        return Result.failure()
    }

    companion object {
        const val INPUT_DATA_URL = "youtube_url"
    }
}
