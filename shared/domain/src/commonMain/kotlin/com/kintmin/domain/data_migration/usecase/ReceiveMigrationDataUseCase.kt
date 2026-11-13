package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary

/** 받는 기기의 이전 한 번. 잔여물 정리 뒤 가져온다. */
class ReceiveMigrationDataUseCase(
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
    private val importMigrationDataUseCase: ImportMigrationDataUseCase,
) {
    suspend operator fun invoke(
        endpoint: MigrationEndpoint,
        token: String,
        onProgress: (MigrationProgress) -> Unit,
    ): Result<MigrationSummary> {
        onProgress(MigrationProgress(MigrationStage.CLEANUP))
        cleanupAnomalousDataUseCase()
        return importMigrationDataUseCase(endpoint, token, onProgress)
    }
}
