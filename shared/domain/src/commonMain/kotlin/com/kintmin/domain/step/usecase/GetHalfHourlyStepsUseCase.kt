package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetHalfHourlyStepsUseCase constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    operator fun invoke(date: String): Flow<List<Int>> {
        return combine(
            stepRepository.getStepDataListByDateFlow(date),
            stepRepository.getLastStepSensorForToday(date),
        ) { stepDataList, latestStepSensor ->
            val mergedStepDataList = mergeLatestStepSensorIfNeeded(
                stepDataList = stepDataList,
                latestStepSensor = latestStepSensor,
            )
            calculateHalfHourlySteps(date, mergedStepDataList)
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

    private fun calculateHalfHourlySteps(date: String, stepDataList: List<StepData>): List<Int> {
        if (stepDataList.isEmpty()) {
            return List(48) { 0 }
        }

        val zoneId = ZoneId.systemDefault()
        val targetDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        val dayStartMillis = targetDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val halfHourMillis = 30 * 60 * 1000L

        val sortedData = stepDataList.sortedBy { it.rawCreatedTime }

        return List(48) { slot ->
            val bucketStart = dayStartMillis + (slot * halfHourMillis)
            val bucketEnd = bucketStart + halfHourMillis

            val bucketData = sortedData
                .asSequence()
                .filter { it.rawCreatedTime in bucketStart..bucketEnd }
                .toList()

            if (slot == 0) {
                calculateStepCountUseCase(bucketData.map { it.stepSensor })
            } else {
                if (bucketData.isEmpty()) {
                    0
                } else {
                    val isFirstDataAtSlotStart = bucketData.first().rawCreatedTime == bucketStart
                    val previousSensor = if (isFirstDataAtSlotStart) {
                        null
                    } else {
                        sortedData
                            .lastOrNull { it.rawCreatedTime < bucketStart }
                            ?.stepSensor
                    }

                    val sensorsForSlot = if (previousSensor != null) {
                        listOf(previousSensor) + bucketData.map { it.stepSensor }
                    } else {
                        bucketData.map { it.stepSensor }
                    }

                    calculateStepCountUseCase(sensorsForSlot)
                }
            }
        }
    }
}
