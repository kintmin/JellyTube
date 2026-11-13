package com.kintmin.ios.port

import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.domain.file_share.usecase.StartPcFileShareServerUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import com.kintmin.presentation.port.FileShareServerController
import com.kintmin.presentation.work_notice.WorkResultNotice
import kotlinx.coroutines.flow.StateFlow

internal class IosFileShareServerController(
    private val startPcFileShareServer: StartPcFileShareServerUseCase,
    private val stopPcFileShareServer: StopPcFileShareServerUseCase,
    fetchPcFileShareServerRunning: FetchPcFileShareServerRunningUseCase,
) : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = fetchPcFileShareServerRunning()

    override fun start() = startPcFileShareServer { result ->
        IosLocalNotification.show(
            WorkResultNotice.Download.Success(
                artist = result.audioMedia.artist,
                name = result.audioMedia.name,
                playlistId = result.playlistIdOnDownload,
                audioMediaId = result.audioMedia.id,
            ),
        )
    }

    override fun stop() = stopPcFileShareServer()
}
