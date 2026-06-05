package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository

class UpdateAccelerateStepUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(accelerateStep: Int): Result<Unit> {
        return stepRepository.updateAccelerateStep(accelerateStep)
    }
}