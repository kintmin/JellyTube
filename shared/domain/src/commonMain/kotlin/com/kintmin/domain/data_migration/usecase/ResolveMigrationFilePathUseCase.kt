package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.repository.DataMigrationRepository

class ResolveMigrationFilePathUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
) {
    operator fun invoke(file: MigrationFile): Result<String> = dataMigrationRepository.resolveFilePath(file)
}
