package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository

class ReplaceStepSensorsUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(startMillis: Long, endMillis: Long, stepDataList: List<StepData>): Result<Unit> {
        return stepRepository.replaceStepSensors(
            startMillis = startMillis,
            endMillis = endMillis,
            stepDataList = stepDataList,
        )
    }
}
