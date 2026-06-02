package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMonthlyDailyStepsUseCase constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, Int>> {
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.BASIC_ISO_DATE
        val today = LocalDate.now(zoneId)
        val todayText = today.format(formatter)

        val monthStart = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return combine(
            stepRepository.getStepDataListInRangeFlow(monthStart, monthEnd),
            stepRepository.getLastStepSensorForToday(todayText),
        ) { allStepData, latestStepSensor ->
            buildDailyStepMap(yearMonth, today, allStepData, latestStepSensor, zoneId)
        }
    }

    private fun buildDailyStepMap(
        yearMonth: YearMonth,
        today: LocalDate,
        allStepData: List<StepData>,
        latestStepSensor: Long?,
        zoneId: ZoneId,
    ): Map<LocalDate, Int> {
        val dailyStepMap = mutableMapOf<LocalDate, Int>()

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            val stepDataList = allStepData.filter {
                it.rawCreatedTime in dayStart..dayEnd
            }

            val sensors = stepDataList
                .sortedBy { it.rawCreatedTime }
                .map { it.stepSensor }
                .toMutableList()

            if (date == today && latestStepSensor != null) {
                sensors += latestStepSensor
            }

            dailyStepMap[date] = calculateStepCountUseCase(sensors)
        }

        return dailyStepMap
    }
}
