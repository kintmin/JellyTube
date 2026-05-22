package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class GetHourlyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    suspend operator fun invoke(date: String): Result<List<Int>> {
        val latestStepSensor = stepRepository.getLastStepSensorForToday(date).firstOrNull()

        return stepRepository.getStepDataListByDate(date)
            .map { stepDataList ->
                val mergedStepDataList = mergeLatestStepSensorIfNeeded(
                    stepDataList = stepDataList,
                    latestStepSensor = latestStepSensor,
                )
                calculateHourlySteps(date, mergedStepDataList)
            }
    }

    private fun mergeLatestStepSensorIfNeeded(
        stepDataList: List<StepData>,
        latestStepSensor: Long?,
    ): List<StepData> {
        if (latestStepSensor == null) return stepDataList

        val mutableList = stepDataList.toMutableList().apply {
            add(
                StepData(
                    rawCreatedTime = System.currentTimeMillis(),
                    stepSensor = latestStepSensor,
                )
            )
        }

        return mutableList
    }

    private fun calculateHourlySteps(date: String, stepDataList: List<StepData>): List<Int> {
        if (stepDataList.isEmpty()) {
            return List(24) { 0 }
        }

        val zoneId = ZoneId.systemDefault()
        val targetDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        val dayStartMillis = targetDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val oneHourMillis = 60 * 60 * 1000L

        val sortedData = stepDataList.sortedBy { it.rawCreatedTime }

        return List(24) { hour ->
            val bucketStart = dayStartMillis + (hour * oneHourMillis)
            val bucketEnd = bucketStart + oneHourMillis

            val bucketData = sortedData
                .asSequence()
                .filter { it.rawCreatedTime in bucketStart..bucketEnd }
                .toList()

            if (hour == 0) {
                calculateStepCountUseCase(bucketData.map { it.stepSensor })
            } else {
                if (bucketData.isEmpty()) {
                    0
                } else {
                    val isFirstDataAtHourStart = bucketData.first().rawCreatedTime == bucketStart
                    val previousSensor = if (isFirstDataAtHourStart) {
                        null
                    } else {
                        sortedData
                            .lastOrNull { it.rawCreatedTime < bucketStart }
                            ?.stepSensor
                    }

                    val sensorsForHour = if (previousSensor != null) {
                        listOf(previousSensor) + bucketData.map { it.stepSensor }
                    } else {
                        bucketData.map { it.stepSensor }
                    }

                    calculateStepCountUseCase(sensorsForHour)
                }
            }
        }
    }
}
