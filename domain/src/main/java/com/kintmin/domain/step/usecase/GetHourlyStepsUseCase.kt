package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetHourlyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(date: String): Result<List<Int>> {
        return stepRepository.getStepDataListByDate(date)
            .map { stepDataList ->
                calculateHourlySteps(stepDataList)
            }
    }

    private fun calculateHourlySteps(stepDataList: List<StepData>): List<Int> {
        if (stepDataList.size < 2) {
            return List(24) { 0 }
        }

        val buckets = Array(24) { mutableListOf<Long>() }

        for (stepData in stepDataList) {
            val hour = Instant.ofEpochMilli(stepData.rawCreatedTime)
                .atZone(ZoneId.systemDefault())
                .hour

            buckets[hour].add(stepData.stepSensor)
        }

        return buckets.map { bucket ->
            calculateSteps(bucket)
        }
    }

    private fun calculateSteps(stepSensors: List<Long>): Int {
        if (stepSensors.size < 2) return 0

        var total = 0L
        var previous = stepSensors[0]

        for (index in 1 until stepSensors.size) {
            val current = stepSensors[index]

            if (current >= previous) {
                total += current - previous
            }

            previous = current
        }

        return total.toInt()
    }
}