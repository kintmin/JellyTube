package com.kintmin.domain.step.usecase

import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first

class GetAccelerateStepUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(): Result<Int> {
        return suspendRunCatching {
            stepRepository.getAccelerateStep().first()!!
        }
    }
}