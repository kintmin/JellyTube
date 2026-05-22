package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetLastStepSensorForTodayUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(today: String): Long? {
        return stepRepository.getLastStepSensorForToday(today).first()
    }
}