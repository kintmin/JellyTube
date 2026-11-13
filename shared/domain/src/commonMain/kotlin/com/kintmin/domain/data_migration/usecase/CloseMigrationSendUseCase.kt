package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository

class CloseMigrationSendUseCase(
    private val repository: MigrationSendServerRepository,
) {
    operator fun invoke() = repository.close()
}
