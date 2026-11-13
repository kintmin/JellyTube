package com.kintmin.domain.file_share.usecase

import com.kintmin.domain.file_share.repository.PcFileShareServerRepository
import kotlinx.coroutines.flow.StateFlow

class FetchPcFileShareServerRunningUseCase(
    private val repository: PcFileShareServerRepository,
) {
    operator fun invoke(): StateFlow<Boolean> = repository.isRunning
}
