package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.backup.repository.BackupRepository

class FindBackupToRestoreUseCase(
    private val backupRepository: BackupRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(): Result<BackupContent?> = backupRepository.readFolderCopy()
        .onFailure { appLog.sendErrorLog(ErrorLog(LOG_TAG, "폴더 백업 읽기 실패", it)) }
}

private const val LOG_TAG = "FindBackupToRestoreUseCase"
