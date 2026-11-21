package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.repository.BackupRepository
import kotlinx.coroutines.flow.Flow

class FetchBackupStatusFlowUseCase(
    private val backupRepository: BackupRepository,
) {
    operator fun invoke(): Flow<BackupStatus> = backupRepository.getStatusFlow()
}
