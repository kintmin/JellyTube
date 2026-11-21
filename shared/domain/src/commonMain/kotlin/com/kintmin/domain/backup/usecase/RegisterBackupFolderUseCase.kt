package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository

/** 폴더를 바꾸면 그 자리에 바로 첫 백업을 쓴다. 등록은 백업이 실패해도 남는다. */
class RegisterBackupFolderUseCase(
    private val backupRepository: BackupRepository,
    private val backupNowUseCase: BackupNowUseCase,
) {
    suspend operator fun invoke(folder: BackupFolder, nowMillis: Long): Result<BackupOutcome> {
        backupRepository.registerFolder(folder).onFailure { return Result.failure(it) }
        return backupNowUseCase(nowMillis)
    }
}
