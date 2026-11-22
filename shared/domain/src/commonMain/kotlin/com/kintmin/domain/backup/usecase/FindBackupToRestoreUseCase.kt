package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.backup.repository.BackupRepository

class FindBackupToRestoreUseCase(
    private val backupRepository: BackupRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(): Result<BackupContent?> {
        val local = backupRepository.readLocalCopy()
            .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "앱 안 백업 사본 읽기 실패", it)) }
        val folder = backupRepository.readFolderCopy()
            .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "폴더 백업 읽기 실패", it)) }
        val found = listOfNotNull(local.getOrNull(), folder.getOrNull()).maxByOrNull { it.createdAtMillis }
        if (found != null) return Result.success(found)
        return local.exceptionOrNull()?.let { Result.failure(it) }
            ?: folder.exceptionOrNull()?.let { Result.failure(it) }
            ?: Result.success(null)
    }
}

private const val LOG_TAG = "FindBackupToRestoreUseCase"
