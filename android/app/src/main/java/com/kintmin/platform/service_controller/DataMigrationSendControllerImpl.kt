package com.kintmin.platform.service_controller

import android.content.Context
import com.kintmin.platform.service.FileShareForegroundService
import com.kintmin.platform.service.migration.MigrationSessionHolder
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.StateFlow

/** PC 파일 수신과 같은 서버를 쓴다. 이전 엔드포인트는 세션이 열려 있을 때만 응답한다. */
class DataMigrationSendControllerImpl(
    private val appContext: Context,
) : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = MigrationSessionHolder.state

    /** PC 수신 때문에 이미 켜져 있던 서버는 이전이 끝나도 끄지 않는다. */
    private var hasStartedServer = false

    override fun start() {
        MigrationSessionHolder.open()
        if (!FileShareForegroundService.isRunning.value) {
            FileShareForegroundService.startService(appContext)
            hasStartedServer = true
        }
    }

    override fun stop() {
        MigrationSessionHolder.close()
        if (hasStartedServer) {
            FileShareForegroundService.stopService(appContext)
            hasStartedServer = false
        }
    }
}
