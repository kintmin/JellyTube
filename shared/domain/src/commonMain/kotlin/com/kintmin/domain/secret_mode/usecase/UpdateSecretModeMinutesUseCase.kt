package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.secret_mode.repository.SecretModeRepository

class UpdateSecretModeMinutesUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
) {
    suspend operator fun invoke(minutes: Int): Result<Unit> = secretModeRepository.updateMinutes(minutes)
}
