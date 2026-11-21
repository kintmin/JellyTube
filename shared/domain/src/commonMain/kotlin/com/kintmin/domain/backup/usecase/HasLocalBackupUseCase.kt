package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.repository.BackupRepository

class HasLocalBackupUseCase(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(): Boolean = backupRepository.hasLocalCopy()
}
