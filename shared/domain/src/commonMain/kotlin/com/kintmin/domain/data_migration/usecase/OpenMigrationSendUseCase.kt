package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.repository.MigrationSendServerRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

class OpenMigrationSendUseCase(
    private val repository: MigrationSendServerRepository,
    private val cleanupAnomalousData: CleanupAnomalousDataUseCase,
    private val exportMigrationData: ExportMigrationDataUseCase,
    private val resolveMigrationFilePath: ResolveMigrationFilePathUseCase,
    private val appLog: AppLog,
) {
    operator fun invoke() = repository.open(
        object : MigrationSendHandler {
            override suspend fun cleanup(): Result<Unit> = cleanupAnomalousData().map { }

            override suspend fun export(): Result<MigrationExport> = exportMigrationData()
                .onFailure { appLog.sendErrorLog(ErrorLog("OpenMigrationSendUseCase", "이전 보내기 목록 만들기 실패", it)) }

            override fun resolveFilePath(file: MigrationFile): Result<String> = resolveMigrationFilePath(file)
        },
    )
}
