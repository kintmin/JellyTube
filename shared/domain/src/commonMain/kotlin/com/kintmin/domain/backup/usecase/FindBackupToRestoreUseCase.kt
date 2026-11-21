package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.repository.BackupRepository

class FindBackupToRestoreUseCase(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Result<BackupContent?> {
        val local = backupRepository.readLocalCopy()
        val folder = backupRepository.readFolderCopy()
        val found = listOfNotNull(local.getOrNull(), folder.getOrNull()).maxByOrNull { it.createdAtMillis }
        if (found != null) return Result.success(found)
        return local.exceptionOrNull()?.let { Result.failure(it) }
            ?: folder.exceptionOrNull()?.let { Result.failure(it) }
            ?: Result.success(null)
    }
}
