package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class GetStepCountUseCase @Inject constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    suspend operator fun invoke(date: String): Int {
        val lastStepSensor = stepRepository.getLastStepSensorForToday(date).firstOrNull()
        val steps = stepRepository.getStepDataListByDate(date).getOrDefault(emptyList()).map {
            it.stepSensor
        }.toMutableList()
        val resultStepList = lastStepSensor?.let {
            steps + it
        } ?: steps
        return calculateStepCountUseCase(resultStepList)
    }
}