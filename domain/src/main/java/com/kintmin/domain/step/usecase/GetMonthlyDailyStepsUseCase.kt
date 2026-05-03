package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

class GetMonthlyDailyStepsUseCase @Inject constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
) {

    suspend operator fun invoke(yearMonth: YearMonth): Result<Map<LocalDate, Int>> {
        val latestStepSensor = stepRepository.getLastStepSensor().firstOrNull()
        val today = LocalDate.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.BASIC_ISO_DATE

        val dailyStepMap = mutableMapOf<LocalDate, Int>()

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateText = date.format(formatter)

            val stepDataList = stepRepository.getStepDataListByDate(dateText)
                .getOrElse { throwable ->
                    return Result.failure(throwable)
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

        return Result.success(dailyStepMap)
    }
}
