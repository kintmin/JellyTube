package com.kintmin.domain.secret_mode.usecase

import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import kotlinx.coroutines.flow.Flow

class FetchSecretModeMinutesFlowUseCase constructor(
    private val secretModeRepository: SecretModeRepository,
) {
    operator fun invoke(): Flow<Int> = secretModeRepository.getMinutesFlow()
}
