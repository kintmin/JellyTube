package com.kintmin.ios.adapter

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.usecase.CloseMigrationSendUseCase
import com.kintmin.domain.data_migration.usecase.FetchMigrationSendStatusUseCase
import com.kintmin.domain.data_migration.usecase.OpenMigrationSendUseCase
import com.kintmin.presentation.port.DataMigrationSendController
import kotlinx.coroutines.flow.StateFlow

/** PC 수신과 같은 서버를 쓴다. 화면을 켜 두는 일은 보내기 화면이 맡는다. */
internal class IosDataMigrationSendController(
    private val openMigrationSend: OpenMigrationSendUseCase,
    private val closeMigrationSend: CloseMigrationSendUseCase,
    fetchMigrationSendStatus: FetchMigrationSendStatusUseCase,
) : DataMigrationSendController {

    override val status: StateFlow<DataMigrationSendStatus> = fetchMigrationSendStatus()

    override fun start(scope: MigrationScope) = openMigrationSend(scope)

    override fun stop() = closeMigrationSend()
}
