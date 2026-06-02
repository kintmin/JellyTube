package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first

class GetLastStepSensorForTodayUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(today: String): Long? {
        return stepRepository.getLastStepSensorForToday(today).first()
    }
}