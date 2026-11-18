package com.kintmin.domain.file_share.usecase

import com.kintmin.domain.file_share.repository.PcFileShareServerRepository

class StopPcFileShareServerUseCase(
    private val repository: PcFileShareServerRepository,
) {
    operator fun invoke() = repository.stop()
}
