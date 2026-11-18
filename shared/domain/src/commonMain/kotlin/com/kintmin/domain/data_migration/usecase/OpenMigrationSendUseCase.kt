package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository

class OpenMigrationSendUseCase(
    private val repository: MigrationSendServerRepository,
    private val cleanupAnomalousData: CleanupAnomalousDataUseCase,
    private val exportMigrationData: ExportMigrationDataUseCase,
    private val resolveMigrationFilePath: ResolveMigrationFilePathUseCase,
) {
    operator fun invoke() = repository.open(
        object : MigrationSendHandler {
            override suspend fun cleanup(): Result<Unit> = cleanupAnomalousData().map { }

            override suspend fun export(): Result<MigrationExport> = exportMigrationData()

            override fun resolveFilePath(file: MigrationFile): Result<String> = resolveMigrationFilePath(file)
        },
    )
}
