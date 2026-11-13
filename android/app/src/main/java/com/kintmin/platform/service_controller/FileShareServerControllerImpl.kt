package com.kintmin.platform.service_controller

import android.content.Context
import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.domain.file_share.usecase.StartPcFileShareServerUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import com.kintmin.platform.push_notification.PushNotificationManager
import com.kintmin.platform.push_notification.notifications.DownloadResultNotification
import com.kintmin.platform.service.FileShareForegroundService
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.work_notice.WorkResultNotice
import kotlinx.coroutines.flow.StateFlow

/** 서버는 data 가 띄우고, 프로세스는 foreground service 가 붙잡아 둔다. */
class FileShareServerControllerImpl(
    private val appContext: Context,
    private val startPcFileShareServer: StartPcFileShareServerUseCase,
    private val stopPcFileShareServer: StopPcFileShareServerUseCase,
    fetchPcFileShareServerRunning: FetchPcFileShareServerRunningUseCase,
    private val pushNotificationManager: PushNotificationManager,
) : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = fetchPcFileShareServerRunning()

    override fun start() {
        startPcFileShareServer { result ->
            pushNotificationManager.sendNotification(
                DownloadResultNotification(
                    WorkResultNotice.Download.Success(
                        artist = result.audioMedia.artist,
                        name = result.audioMedia.name,
                        playlistId = result.playlistIdOnDownload,
                        audioMediaId = result.audioMedia.id,
                    ),
                ),
            )
        }
        FileShareForegroundService.startService(appContext, FileShareForegroundService.Owner.PC_FILE_SHARE)
            .onFailure { stopPcFileShareServer() }
    }

    override fun stop() {
        stopPcFileShareServer()
        FileShareForegroundService.stopService(appContext, FileShareForegroundService.Owner.PC_FILE_SHARE)
    }
}
