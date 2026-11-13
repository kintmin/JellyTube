package com.kintmin.platform.service_controller

import android.content.Context
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.usecase.CloseMigrationSendUseCase
import com.kintmin.domain.data_migration.usecase.FetchMigrationSendStatusUseCase
import com.kintmin.domain.data_migration.usecase.OpenMigrationSendUseCase
import com.kintmin.platform.service.FileShareForegroundService
import com.kintmin.presentation.port.DataMigrationSendController
import kotlinx.coroutines.flow.StateFlow

/** PC 파일 수신과 같은 서버를 쓴다. 이전 엔드포인트는 세션이 열려 있을 때만 응답한다. */
class DataMigrationSendControllerImpl(
    private val appContext: Context,
    private val openMigrationSend: OpenMigrationSendUseCase,
    private val closeMigrationSend: CloseMigrationSendUseCase,
    fetchMigrationSendStatus: FetchMigrationSendStatusUseCase,
) : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = fetchMigrationSendStatus()

    override fun start() {
        openMigrationSend()
        FileShareForegroundService.startService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION)
            .onFailure { closeMigrationSend() }
    }

    override fun stop() {
        closeMigrationSend()
        FileShareForegroundService.stopService(appContext, FileShareForegroundService.Owner.DATA_MIGRATION)
    }
}
