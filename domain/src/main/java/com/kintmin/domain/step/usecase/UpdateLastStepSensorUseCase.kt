package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import javax.inject.Inject

class UpdateLastStepSensorUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(lastStepSensor: Long): Result<Unit> {
        return stepRepository.updateLastStepSensor(lastStepSensor)
    }
}