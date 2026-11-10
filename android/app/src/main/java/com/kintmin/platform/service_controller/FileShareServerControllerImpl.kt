package com.kintmin.platform.service_controller

import android.content.Context
import com.kintmin.platform.service.FileShareForegroundService
import com.kintmin.presentation.port.FileShareServerController
import kotlinx.coroutines.flow.StateFlow

/** 파일 수신 서버를 foreground service 로 수행한다. */
class FileShareServerControllerImpl(
    private val appContext: Context,
) : FileShareServerController {

    override val isRunning: StateFlow<Boolean> = FileShareForegroundService.isRunning

    override fun start() {
        FileShareForegroundService.startService(appContext)
    }

    override fun stop() {
        FileShareForegroundService.stopService(appContext)
    }
}
