package com.kintmin.domain.step.usecase

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResetDataOncePerDayUseCase @Inject constructor(
    private val registerDailyResetWorkerUseCase: RegisterDailyResetWorkerUseCase,
) {

    internal val cachedEpochDay = AtomicLong(LocalDate.now(ZoneId.systemDefault()).toEpochDay())

    /**
     * 원자성 보장:
     * - AtomicLong.compareAndSet으로 중복 초기화 방지
     * - resetAction을 즉시 동기 호출하여 코루틴 지연으로 인한 일시적 0걸음 이슈 방지
     * - registerDailyResetWorkerUseCase는 WorkManager 등록(동기 API)이므로 직접 호출
     */
    operator fun invoke(currentStep: Int, currentStepSensor: Long?, zoneId: ZoneId, resetAction: () -> Unit) {
        val todayEpochDay = LocalDate.now(zoneId).toEpochDay()
        val prevDay = cachedEpochDay.get()
        if (prevDay >= todayEpochDay) return
        if (!cachedEpochDay.compareAndSet(prevDay, todayEpochDay)) return

        val targetDate = LocalDate.ofEpochDay(prevDay).format(DateTimeFormatter.BASIC_ISO_DATE)
        resetAction()
        registerDailyResetWorkerUseCase(targetDate, currentStep, currentStepSensor)
    }
}
