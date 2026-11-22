package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.BackupSchedule
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/**
 * 저절로 도는 백업. 쓸 때가 아니면 아무것도 하지 않고 null 을 돌려준다. 백업은 하나뿐이라, 복원을 기다리는
 * 백업을 덜 채워진 이 기기 상태로 덮지 않는다 — 그런 동안에는 사용자가 복원하거나 직접 백업할 때까지 쉰다.
 */
class BackupIfDueUseCase(
    private val backupRepository: BackupRepository,
    private val dataMigrationRepository: DataMigrationRepository,
    private val backupNowUseCase: BackupNowUseCase,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<BackupOutcome?> {
        val status = backupRepository.getStatusFlow().first()
        if (status.folder == null) return Result.success(null)
        if (!BackupSchedule.isDue(status.lastSuccessMillis, nowMillis, timeZone)) return Result.success(null)
        // 폴더에 쓸 수 없는 동안 앱이 내려갈 때마다 다시 만들지 않는다.
        if (status.lastFailureMillis?.let { !BackupSchedule.isDue(it, nowMillis, timeZone) } == true) return Result.success(null)
        if (isRestorePending(status)) return Result.success(null)
        return backupNowUseCase(nowMillis)
    }

    /** 이전·복원이 임시 공간에 받아 둔 것이 있거나, 이 설치가 아직 백업한 적 없이 남은 백업을 찾았다. */
    private suspend fun isRestorePending(status: BackupStatus): Boolean {
        val staged = dataMigrationRepository.fetchStagedFiles()
            .onFailure { logSkip("받아 둔 이전 파일 읽기 실패", it) }
        if (staged.getOrNull()?.isEmpty() != true) return true
        if (status.lastSuccessMillis != null) return false
        if (backupRepository.hasLocalCopy()) return true
        val folderCopy = backupRepository.readFolderCopy()
            .onFailure { logSkip("폴더 백업 읽기 실패", it) }
        return folderCopy.isFailure || folderCopy.getOrNull() != null
    }

    private fun logSkip(message: String, error: Throwable) {
        appLog.sendErrorLog(ErrorLog("BackupIfDueUseCase", "$message — 남은 백업을 덮지 않으려 자동 백업을 쉼", error))
    }
}
