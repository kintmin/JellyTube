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

    override fun start() {
        MigrationSessionHolder.open()
        FileShareForegroundService.startService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION)
    }

    override fun stop() {
        MigrationSessionHolder.close()
        FileShareForegroundService.stopService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION)
    }
}
