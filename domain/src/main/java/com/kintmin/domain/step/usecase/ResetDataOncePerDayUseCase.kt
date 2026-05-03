package com.kintmin.domain.step.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResetDataOncePerDayUseCase @Inject constructor(
    private val registerDailyResetWorkerUseCase: RegisterDailyResetWorkerUseCase,
) {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    operator fun invoke(currentStep: Int, currentStepSensor: Long?, zoneId: ZoneId, resetAction: () -> Unit) {
        val todayEpochDay = LocalDate.now(zoneId).toEpochDay()
        if (cachedEpochDay >= todayEpochDay) return

        scope.launch {
            mutex.withLock {
                resetDataAtomically(currentStep, currentStepSensor, zoneId, resetAction)
            }
        }
    }

    /**
     * 원자성 보장:
     * 1. suspend가 되어선 안된다. (코루틴 cancel 전파로 인해 중도 실패 시 원자성이 깨짐)
     * 2. launch 등으로 코루틴을 생성해선 안된다. (원자성에 어긋남. 필요 시 registerDailyResetWorkerUseCase 에서 worker 등록)
     */
    private fun resetDataAtomically(
        currentStep: Int,
        currentStepSensor: Long?,
        zoneId: ZoneId,
        resetAction: () -> Unit
    ) {
        runCatching {
            // 다수의 코루틴 생성 시 중복 방지
            val todayEpochDay = LocalDate.now(zoneId).toEpochDay()
            if (cachedEpochDay >= todayEpochDay) return

            val targetDate = LocalDate.ofEpochDay(cachedEpochDay).format(DateTimeFormatter.BASIC_ISO_DATE)
            resetAction()
            registerDailyResetWorkerUseCase(targetDate, currentStep, currentStepSensor)

            cachedEpochDay = todayEpochDay
        }
    }
}