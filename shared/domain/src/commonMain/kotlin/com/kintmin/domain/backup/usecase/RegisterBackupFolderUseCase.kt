package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository

/** 폴더를 바꾸면 그 자리에 바로 첫 백업을 쓴다. 등록은 백업이 실패해도 남는다. */
class RegisterBackupFolderUseCase(
    private val backupRepository: BackupRepository,
    private val backupNowUseCase: BackupNowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(folder: BackupFolder, nowMillis: Long): Result<BackupOutcome> {
        backupRepository.registerFolder(folder).onFailure { return Result.failure(it) }
        appLog.sendDebugLog(DebugLog("RegisterBackupFolderUseCase", "백업 폴더 등록: $folder"))
        return backupNowUseCase(nowMillis)
    }
}
