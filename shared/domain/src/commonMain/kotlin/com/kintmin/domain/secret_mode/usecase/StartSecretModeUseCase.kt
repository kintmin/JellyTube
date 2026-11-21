package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.secret_mode.repository.SecretModeRepository

class StartSecretModeUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
) {
    suspend operator fun invoke() = secretModeRepository.setEnabled(true)
}
