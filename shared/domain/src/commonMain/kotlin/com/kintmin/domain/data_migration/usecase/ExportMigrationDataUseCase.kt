package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.repository.DataMigrationRepository

class ExportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
) {
    suspend operator fun invoke(): Result<MigrationExport> = dataMigrationRepository.exportData()
}
