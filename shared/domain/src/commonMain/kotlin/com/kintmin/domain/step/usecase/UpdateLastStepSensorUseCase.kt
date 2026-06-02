package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.extension.todayLocalDate
import com.kintmin.domain.step.repository.StepRepository

class UpdateLastStepSensorUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(lastStepSensor: Long): Result<Unit> {
        val today = todayLocalDate().toBasicIsoString()
        stepRepository.updateLastStepSensorDate(today)
        return stepRepository.updateLastStepSensor(lastStepSensor)
    }
}
