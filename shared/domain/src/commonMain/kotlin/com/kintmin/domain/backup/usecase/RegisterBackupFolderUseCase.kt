package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository
import kotlinx.coroutines.flow.first

/**
 * 폴더를 바꾸면 그 자리에 바로 첫 백업을 쓴다. 등록은 백업이 실패해도 남는다.
 * 재설치 뒤 백업이 든 폴더를 다시 고른 것이면 그 백업을 복원할 수 있게 덮지 않는다.
 */
class RegisterBackupFolderUseCase(
    private val backupRepository: BackupRepository,
    private val backupNowUseCase: BackupNowUseCase,
) {
    suspend operator fun invoke(folder: BackupFolder, nowMillis: Long): Result<BackupOutcome> {
        backupRepository.registerFolder(folder).onFailure { return Result.failure(it) }
        val hasNeverBackedUp = backupRepository.getStatusFlow().first().lastSuccessMillis == null
        // 읽지 못하면 있는 것으로 본다 — 없다고 보면 남은 백업을 덮는다.
        if (hasNeverBackedUp && backupRepository.readFolderCopy().let { it.isFailure || it.getOrNull() != null }) {
            return Result.success(BackupOutcome.KEPT_EXISTING)
        }
        return backupNowUseCase(nowMillis)
    }
}
