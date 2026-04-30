package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetLastStepSensorUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(): Result<Long> {
        return runCatching {
            stepRepository.getLastStepSensor().first()!!
        }
    }
}