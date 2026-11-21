package com.kintmin.domain.backup.usecase

import com.kintmin.domain.backup.BackupSchedule
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** 폴더를 고르지 않았거나 오늘 이미 썼으면 아무것도 하지 않고 null 을 돌려준다. */
class BackupIfDueUseCase(
    private val backupRepository: BackupRepository,
    private val backupNowUseCase: BackupNowUseCase,
) {
    suspend operator fun invoke(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<BackupOutcome?> {
        val status = backupRepository.getStatusFlow().first()
        if (status.folder == null) return Result.success(null)
        if (!BackupSchedule.isDue(status.lastSuccessMillis, nowMillis, timeZone)) return Result.success(null)
        return backupNowUseCase(nowMillis)
    }
}
