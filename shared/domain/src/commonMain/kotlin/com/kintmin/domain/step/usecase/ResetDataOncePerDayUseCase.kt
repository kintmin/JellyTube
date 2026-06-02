package com.kintmin.domain.step.usecase

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

class ResetDataOncePerDayUseCase constructor(
    private val registerDailyResetWorkerUseCase: RegisterDailyResetWorkerUseCase,
) {

    internal val cachedEpochDay = AtomicLong(LocalDate.now(ZoneId.systemDefault()).toEpochDay())

    /**
     * ?ìž??ë³´ìž¥:
     * - AtomicLong.compareAndSet?¼ë¡œ ì¤‘ë³µ ì´ˆê¸°??ë°©ì?
     * - resetAction??ì¦‰ì‹œ ?™ê¸° ?¸ì¶œ?˜ì—¬ ì½”ë£¨??ì§€?°ìœ¼ë¡??¸í•œ ?¼ì‹œ??0ê±¸ìŒ ?´ìŠˆ ë°©ì?
     * - registerDailyResetWorkerUseCase??WorkManager ?±ë¡(?™ê¸° API)?´ë?ë¡?ì§ì ‘ ?¸ì¶œ
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
