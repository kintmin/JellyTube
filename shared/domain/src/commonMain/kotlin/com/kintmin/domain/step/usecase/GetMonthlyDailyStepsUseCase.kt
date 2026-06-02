package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.startOfDayMillis
import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.extension.todayLocalDate
import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.model.YearMonth
import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

class GetMonthlyDailyStepsUseCase constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    operator fun invoke(yearMonth: YearMonth): Flow<Map<LocalDate, Int>> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = todayLocalDate(timeZone)
        val todayText = today.toBasicIsoString()

        val monthStart = yearMonth.atDay(1).startOfDayMillis(timeZone)
        val monthEnd = yearMonth.atEndOfMonth.plus(1, DateTimeUnit.DAY).startOfDayMillis(timeZone)

        return combine(
            stepRepository.getStepDataListInRangeFlow(monthStart, monthEnd),
            stepRepository.getLastStepSensorForToday(todayText),
        ) { allStepData, latestStepSensor ->
            buildDailyStepMap(yearMonth, today, allStepData, latestStepSensor, timeZone)
        }
    }

    private fun buildDailyStepMap(
        yearMonth: YearMonth,
        today: LocalDate,
        allStepData: List<StepData>,
        latestStepSensor: Long?,
        timeZone: TimeZone,
    ): Map<LocalDate, Int> {
        val dailyStepMap = mutableMapOf<LocalDate, Int>()

        for (day in 1..yearMonth.lengthOfMonth) {
            val date = yearMonth.atDay(day)
            val dayStart = date.startOfDayMillis(timeZone)
            val dayEnd = date.plus(1, DateTimeUnit.DAY).startOfDayMillis(timeZone)

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
