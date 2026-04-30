package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import javax.inject.Inject

class DeleteOldStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(): Result<Unit> {
        return stepRepository.deleteEntitiesOlderThan15DaysFromTodayMidnight()
    }
}