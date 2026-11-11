package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.repository.DataMigrationRepository

class PairMigrationSourceUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
) {
    suspend operator fun invoke(endpoint: MigrationEndpoint, code: String): Result<String> =
        dataMigrationRepository.pair(endpoint, code.trim())
}
