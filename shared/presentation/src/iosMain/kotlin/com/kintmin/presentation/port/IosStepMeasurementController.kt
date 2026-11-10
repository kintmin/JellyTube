package com.kintmin.presentation.port

import com.kintmin.domain.extension.startOfDayMillis
import com.kintmin.domain.extension.todayLocalDate
import com.kintmin.domain.step.usecase.BackupStepSensorUseCase
import com.kintmin.domain.step.usecase.UpdateLastStepSensorUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSTimeIntervalSince1970
import kotlin.coroutines.resume
import kotlin.time.Clock

/** 소급 조회로 메울 날짜 수. `CMPedometer` 는 최대 7일을 들고 있다. */
private const val BACKFILL_DAYS = 2

/**
 * `CMPedometer` 기반 걸음수 측정. 적는 시점과 값은 [StepSlotSchedule] 이 정하고
 * `StepSlotScheduleTest` 가 고정한다.
 *
 * 모션 코프로세서가 앱과 무관하게 걸음을 기록하고 소급 조회가 되므로, Android 의 포그라운드
 * 서비스에 해당하는 장치가 필요 없다. 켤 때 지나간 구간을 메우고 떠 있는 동안만 실시간으로 잇는다.
 */
internal class IosStepMeasurementController(
    private val backupStepSensorUseCase: BackupStepSensorUseCase,
    private val updateLastStepSensorUseCase: UpdateLastStepSensorUseCase,
) : StepMeasurementController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pedometer = CMPedometer()

    private var isRunning = false

    private var liveDayStartMillis: Long? = null
    private var lastWrittenBoundaryMillis: Long? = null

    override fun start() {
        if (isRunning) return
        // 권한 요청은 설정 화면이 한다. 여기서 창을 띄우지 않는다.
        if (!CMPedometer.isStepCountingAvailable()) return
        if (CMPedometer.authorizationStatus() != CMAuthorizationStatusAuthorized) return

        isRunning = true
        scope.launch {
            backfill()
            startLiveUpdates()
        }
    }

    override fun stop() {
        if (!isRunning) return

        isRunning = false
        liveDayStartMillis = null
        lastWrittenBoundaryMillis = null
        pedometer.stopPedometerUpdates()
    }

    /** 같은 경계를 다시 적어도 PK 충돌이 REPLACE 라 안전하다. */
    private suspend fun backfill() {
        val timeZone = TimeZone.currentSystemDefault()
        val today = todayLocalDate(timeZone)
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        for (dayOffset in (BACKFILL_DAYS - 1) downTo 0) {
            val dayStartMillis = today.minus(dayOffset, DateTimeUnit.DAY).startOfDayMillis(timeZone)

            for (point in StepSlotSchedule.forDay(dayStartMillis, nowMillis)) {
                val steps = point.queryUntilMillis
                    ?.let { cumulativeStepsSince(dayStartMillis, it) ?: break }
                    ?: 0L
                backupStepSensorUseCase(stepSensor = steps, rawCreatedTime = point.rawCreatedTime)

                if (dayOffset == 0 && point.queryUntilMillis == point.rawCreatedTime) {
                    lastWrittenBoundaryMillis = point.rawCreatedTime
                }
            }
        }
    }

    private fun startLiveUpdates() {
        val timeZone = TimeZone.currentSystemDefault()
        val dayStartMillis = todayLocalDate(timeZone).startOfDayMillis(timeZone)
        liveDayStartMillis = dayStartMillis

        pedometer.startPedometerUpdatesFromDate(dayStartMillis.toNSDate()) { data, _ ->
            val cumulative = data?.numberOfSteps?.longValue ?: return@startPedometerUpdatesFromDate
            scope.launch { onLiveUpdate(cumulative) }
        }
    }

    private suspend fun onLiveUpdate(cumulativeSinceDayStart: Long) {
        val timeZone = TimeZone.currentSystemDefault()
        val dayStartMillis = todayLocalDate(timeZone).startOfDayMillis(timeZone)

        // 자정을 넘겼다. 누적 기준이 바뀌었으므로 어제를 마감하고 다시 건다.
        if (dayStartMillis != liveDayStartMillis) {
            stop()
            start()
            return
        }

        updateLastStepSensorUseCase(cumulativeSinceDayStart)
        writeCrossedBoundaries(dayStartMillis)
    }

    private suspend fun writeCrossedBoundaries(dayStartMillis: Long) {
        val boundaries = StepSlotSchedule.boundaries(
            dayStartMillis = dayStartMillis,
            afterMillis = lastWrittenBoundaryMillis ?: dayStartMillis,
            nowMillis = Clock.System.now().toEpochMilliseconds(),
        )

        for (boundaryMillis in boundaries) {
            val cumulative = cumulativeStepsSince(dayStartMillis, boundaryMillis) ?: return
            backupStepSensorUseCase(stepSensor = cumulative, rawCreatedTime = boundaryMillis)
            lastWrittenBoundaryMillis = boundaryMillis
        }
    }

    private suspend fun cumulativeStepsSince(fromMillis: Long, toMillis: Long): Long? =
        suspendCancellableCoroutine { continuation ->
            pedometer.queryPedometerDataFromDate(fromMillis.toNSDate(), toMillis.toNSDate()) { data, _ ->
                continuation.resume(data?.numberOfSteps?.longValue)
            }
        }
}

private fun Long.toNSDate(): NSDate =
    NSDate(timeIntervalSinceReferenceDate = this / 1000.0 - NSTimeIntervalSince1970)
