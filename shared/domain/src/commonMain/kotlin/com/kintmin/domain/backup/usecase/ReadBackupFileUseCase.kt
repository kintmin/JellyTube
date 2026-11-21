package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.repository.BackupRepository

class ReadBackupFileUseCase(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(fileId: String): Result<BackupContent> = backupRepository.read(fileId)
}
