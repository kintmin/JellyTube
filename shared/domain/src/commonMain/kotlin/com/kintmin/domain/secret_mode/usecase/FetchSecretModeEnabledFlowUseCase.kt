package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import kotlinx.coroutines.flow.StateFlow

class FetchSecretModeEnabledFlowUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
) {
    operator fun invoke(): StateFlow<Boolean> = secretModeRepository.isEnabledFlow
}
