package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.toBasicIsoString
import kotlinx.atomicfu.atomic
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ResetDataOncePerDayUseCase constructor(
    private val registerDailyResetWorkerUseCase: RegisterDailyResetWorkerUseCase,
) {

    internal val cachedEpochDay = atomic(
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays()
            .toLong()
    )

    /**
     * 날짜 보장:
     * - atomic.compareAndSet으로 중복 초기화 방지
     * - resetAction은 즉시 동기 호출하여 코루틴 지연으로 인한 임시 0걸음 이슈 방지
     * - registerDailyResetWorkerUseCase는 WorkManager 등록(비기 API)을 직접 호출
     */
    operator fun invoke(
        currentStep: Int,
        currentStepSensor: Long?,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        resetAction: () -> Unit,
    ) {
        val today: LocalDate = Clock.System.now().toLocalDateTime(timeZone).date
        val todayEpochDay = today.toEpochDays().toLong()
        val prevDay = cachedEpochDay.value
        if (prevDay >= todayEpochDay) return
        if (!cachedEpochDay.compareAndSet(prevDay, todayEpochDay)) return

        val targetDate = LocalDate.fromEpochDays(prevDay.toInt()).toBasicIsoString()
        resetAction()
        registerDailyResetWorkerUseCase(targetDate, currentStep, currentStepSensor)
    }
}
