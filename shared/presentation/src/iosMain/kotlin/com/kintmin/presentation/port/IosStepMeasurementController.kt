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

/** 차트 한 칸. `GetHalfHourlyStepsUseCase` 가 하루를 48칸으로 나눈다. */
private const val SLOT_MILLIS = 30L * 60L * 1000L
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

/**
 * 소급 조회로 메울 날짜 수. 어제까지 메우면 하루가 끝날 때 마지막 칸이 비는 일이 없다.
 *
 * `CMPedometer` 는 최대 7일을 들고 있으므로 늘리는 것은 숫자 하나다. 2일로 둔 것은 앱을
 * 매일 여는 사용을 전제했기 때문이다.
 */
private const val BACKFILL_DAYS = 2

/**
 * `CMPedometer` 기반 걸음수 측정.
 *
 * **Android 와 달리 상시 수집이 필요 없다.** 모션 코프로세서가 앱과 무관하게 걸음을 기록하고
 * [CMPedometer.queryPedometerDataFromDate] 로 소급 조회할 수 있어서, 포그라운드 서비스에
 * 해당하는 장치가 iOS 에는 애초에 필요하지 않다. 켤 때 지나간 구간을 메우고, 떠 있는 동안만
 * 실시간으로 잇는다.
 *
 * ### 저장 규약
 *
 * `STEP` 테이블은 `(rawCreatedTime, stepSensor)` 이고 화면은 **이웃한 두 값의 차이**만 쓴다
 * (`CalculateStepCountUseCase`). 그래서 절대값의 기준이 무엇이든 하루 안에서 단조증가이기만
 * 하면 된다. 여기서는 **그날 0시부터의 누적**을 쓴다.
 *
 * 경계 두 곳이 중요하다.
 * - **0시에 0 을 하나 심는다.** 첫 칸은 시작과 끝 두 값이 다 있어야 차이를 낼 수 있다.
 * - **지나간 날의 마감은 자정 1ms 전에 적는다.** Android 는 센서값이 부팅 기준이라 자정에
 *   적은 한 점이 전날 마지막 칸과 다음 날 첫 칸 양쪽을 겸하지만, 하루 기준 누적은 자정에
 *   0 으로 떨어지므로 같은 자리에 적으면 둘 중 하나가 깨진다.
 */
internal class IosStepMeasurementController(
    private val backupStepSensorUseCase: BackupStepSensorUseCase,
    private val updateLastStepSensorUseCase: UpdateLastStepSensorUseCase,
) : StepMeasurementController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pedometer = CMPedometer()

    private var isRunning = false

    /** 실시간 갱신이 기준으로 삼은 날의 0시. 자정을 넘겼는지 이것으로 안다. */
    private var liveDayStartMillis: Long? = null

    /** 오늘 마지막으로 적은 경계. 여기서부터 이어서 적는다. */
    private var lastWrittenBoundaryMillis: Long? = null

    override fun start() {
        if (isRunning) return
        // 기기에 만보계가 없거나(시뮬레이터) 권한이 없으면 조용히 접는다.
        // 권한 요청은 설정 화면이 한다 — 측정을 거는 자리에서 창을 띄우지 않는다.
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

    /** 앱이 꺼져 있던 구간을 메운다. 같은 경계를 다시 적어도 PK 충돌이 REPLACE 라 안전하다. */
    private suspend fun backfill() {
        val timeZone = TimeZone.currentSystemDefault()
        val today = todayLocalDate(timeZone)
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        for (dayOffset in (BACKFILL_DAYS - 1) downTo 0) {
            val dayStartMillis = today.minus(dayOffset, DateTimeUnit.DAY).startOfDayMillis(timeZone)
            if (dayStartMillis > nowMillis) continue

            backupStepSensorUseCase(stepSensor = 0L, rawCreatedTime = dayStartMillis)

            val dayEndMillis = dayStartMillis + DAY_MILLIS
            var boundaryMillis = dayStartMillis + SLOT_MILLIS
            while (boundaryMillis < dayEndMillis && boundaryMillis <= nowMillis) {
                val cumulative = cumulativeStepsSince(dayStartMillis, boundaryMillis) ?: break
                backupStepSensorUseCase(stepSensor = cumulative, rawCreatedTime = boundaryMillis)
                if (dayOffset == 0) lastWrittenBoundaryMillis = boundaryMillis
                boundaryMillis += SLOT_MILLIS
            }

            if (dayEndMillis <= nowMillis) {
                val closing = cumulativeStepsSince(dayStartMillis, dayEndMillis) ?: continue
                backupStepSensorUseCase(stepSensor = closing, rawCreatedTime = dayEndMillis - 1)
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

        // 화면은 이 값을 아직 경계에 닿지 않은 마지막 칸으로 쓴다.
        updateLastStepSensorUseCase(cumulativeSinceDayStart)
        writeCrossedBoundaries(dayStartMillis)
    }

    /** 떠 있는 동안 30분 경계를 넘었으면 그 지점의 누적을 적는다. */
    private suspend fun writeCrossedBoundaries(dayStartMillis: Long) {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        var boundaryMillis = (lastWrittenBoundaryMillis ?: dayStartMillis) + SLOT_MILLIS

        while (boundaryMillis <= nowMillis && boundaryMillis < dayStartMillis + DAY_MILLIS) {
            val cumulative = cumulativeStepsSince(dayStartMillis, boundaryMillis) ?: return
            backupStepSensorUseCase(stepSensor = cumulative, rawCreatedTime = boundaryMillis)
            lastWrittenBoundaryMillis = boundaryMillis
            boundaryMillis += SLOT_MILLIS
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
