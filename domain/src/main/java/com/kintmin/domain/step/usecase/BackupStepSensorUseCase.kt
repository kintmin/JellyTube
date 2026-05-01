package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import javax.inject.Inject

class BackupStepSensorUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(stepSensor: Long, rawCreatedTime: Long = System.currentTimeMillis()): Result<Unit> {
        return stepRepository.insertStepSensor(
            rawCreatedTime = rawCreatedTime,
            stepSensor = stepSensor,
        )
    }
}