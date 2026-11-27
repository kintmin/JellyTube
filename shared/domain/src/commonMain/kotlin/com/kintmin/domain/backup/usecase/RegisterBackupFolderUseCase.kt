package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository

/**
 * 폴더를 바꾸면 그 자리에 바로 첫 백업을 쓴다. 등록은 백업이 실패해도 남는다.
 *
 * **고른 폴더에 되돌릴 백업이 있으면 쓰지 않는다.** 오늘 날짜로 새로 쓰는 순간 날짜를 모르는 옛
 * 백업이 후보에서 밀려 되돌릴 길이 막힌다 — 폴더를 고르는 것은 흔히 **되돌리려고** 하는 일이다.
 */
class RegisterBackupFolderUseCase(
    private val backupRepository: BackupRepository,
    private val backupNowUseCase: BackupNowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(folder: BackupFolder, nowMillis: Long): Result<BackupOutcome> {
        backupRepository.registerFolder(folder).onFailure { return Result.failure(it) }
        appLog.sendDebugLog(DebugLog(LOG_TAG, "백업 폴더 등록: $folder"))

        val folderCopy = backupRepository.readFolderCopy()
        folderCopy.onFailure {
            appLog.sendErrorLog(ErrorLog(LOG_TAG, "고른 폴더의 백업 읽기 실패: $folder", it))
            return Result.success(BackupOutcome.KEPT_UNREADABLE)
        }
        if (folderCopy.getOrNull() != null) return Result.success(BackupOutcome.KEPT_EXISTING)

        return backupNowUseCase(nowMillis)
    }
}

private const val LOG_TAG = "RegisterBackupFolderUseCase"
