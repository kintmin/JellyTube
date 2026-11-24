package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlin.time.Duration

class StartSecretModeUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(duration: Duration) {
        secretModeRepository.setEnabled(true)
        appLog.sendDebugLog(DebugLog("SecretMode", "비밀모드 켬: ${duration.inWholeMinutes}분"))
    }
}
