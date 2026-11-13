package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.step.DailyResetMark
import com.kintmin.domain.step.ScheduleDailyStepClosing
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ResetDataOncePerDayUseCase constructor(
    private val scheduleDailyStepClosing: ScheduleDailyStepClosing,
) {

    /** [resetAction] 은 즉시 동기로 부른다. 코루틴으로 미루면 그 사이 0 걸음이 잠깐 보인다. */
    operator fun invoke(
        mark: DailyResetMark,
        currentStep: Int,
        currentStepSensor: Long?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        resetAction: () -> Unit,
    ) {
        val today = Clock.System.now().toLocalDateTime(timeZone).date.toEpochDays().toLong()
        val previousDay = mark.claim(today) ?: return

        resetAction()
        scheduleDailyStepClosing(LocalDate.fromEpochDays(previousDay.toInt()).toBasicIsoString(), currentStep, currentStepSensor)
    }
}
