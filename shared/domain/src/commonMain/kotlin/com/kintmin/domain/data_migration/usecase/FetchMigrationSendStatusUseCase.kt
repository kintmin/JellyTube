package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import kotlinx.coroutines.flow.StateFlow

class FetchMigrationSendStatusUseCase(
    private val repository: MigrationSendServerRepository,
) {
    operator fun invoke(): StateFlow<DataMigrationSendStatus> = repository.status
}
