package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import javax.inject.Inject

class UpdateTodayStepCountUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(todayStepCount: Int): Result<Unit> {
        return stepRepository.updateTodayStepCount(todayStepCount)
    }
}