package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetTodayStepCountUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(): Result<Int> {
        return runCatching {
            stepRepository.getTodayStepCount().first()!!
        }
    }
}