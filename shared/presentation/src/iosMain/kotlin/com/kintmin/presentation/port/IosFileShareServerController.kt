package com.kintmin.presentation.port

import com.kintmin.domain.file_share.usecase.FetchPcFileShareServerRunningUseCase
import com.kintmin.domain.file_share.usecase.StartPcFileShareServerUseCase
import com.kintmin.domain.file_share.usecase.StopPcFileShareServerUseCase
import com.kintmin.presentation.platform.ScreenAwake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class IosFileShareServerController(
    private val startPcFileShareServer: StartPcFileShareServerUseCase,
    private val stopPcFileShareServer: StopPcFileShareServerUseCase,
    fetchPcFileShareServerRunning: FetchPcFileShareServerRunningUseCase,
) : FileShareServerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val isRunning: StateFlow<Boolean> = fetchPcFileShareServerRunning()

    init {
        // 수신 화면이 아니라 서버에 묶는다. 다른 화면에서도 자동 잠금이 오면 서스펜드되어 서버가 멈춘다.
        scope.launch { ScreenAwake.holdWhile(isRunning) }
    }

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
