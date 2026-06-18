package com.kintmin.platform.service

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class StepSensorProcessor(
    private val backupUnitMillis: Long,
    private val getTimeZone: () -> TimeZone,
    private val onBackupSensor: (stepSensor: Long, saveMillis: Long) -> Unit,
    private val onDailyReset: () -> Unit,
) {

    var lastBackupUnitMillis: Long? = null

    /**
     * https://source.android.com/docs/core/interaction/sensors/sensor-types#step_counter
     * https://source.android.com/docs/core/interaction/sensors/report-modes#on-change
     *
     * 걸음수 센서는 Reporting-mode: On-change 이고, 센서에 register할 때 무조건 1번 이벤트를 받는다.
     * (변경으로 인한 트리거가 아닌 활성화로 인한 트리거)
     */
    fun updateStep(
        prevStepSensor: Long?,
        newStepSensor: Long,
        currentMillis: Long,
        todayLastSavedStepSensor: Long? = null,
        onStepDelta: (Int) -> Unit,
    ) {
        // prevStepSensor는 in-memory 캐시로, 서비스 시작 직후에는 항상 null
        val isNewlyStartedForeground = prevStepSensor == null
        if (isNewlyStartedForeground) {
            if (todayLastSavedStepSensor != null) {
                val delta = if (newStepSensor >= todayLastSavedStepSensor) {
                    (newStepSensor - todayLastSavedStepSensor).toInt()  // 서비스 꺼짐 ~ 재시작: 센서 기반 복원
                } else {
                    newStepSensor.toInt()  // 재부팅: 센서가 0부터 리셋됨
                }
                if (delta > 0) onStepDelta(delta)
            }
            forceBackupWithCurrentTime(newStepSensor, currentMillis)
        } else {
            val stepDelta = (newStepSensor - prevStepSensor).toInt()
            onStepDelta(stepDelta)
            backupStepSensor(
                stepSensor = prevStepSensor,
                currentMillis = currentMillis,
            )
        }
    }

    /**
     * 버킷 단위와 무관하게 현재 시각으로 기준점을 저장한다.
     * - 재부팅/첫 시작: 기준 센서값이 달라진 시점이므로 즉시 저장
     * - 시간/타임존 변경: 기준 시각이 바뀌므로 즉시 저장
     */
    fun updateStepAfterBoot(
        newStepSensor: Long,
        currentMillis: Long,
        onStepDelta: (Int) -> Unit,
    ) {
        val delta = newStepSensor.toInt()
        if (delta > 0) onStepDelta(delta)
        forceBackupWithCurrentTime(newStepSensor, currentMillis)
    }

    fun forceBackupWithCurrentTime(stepSensor: Long, currentMillis: Long) {
        onBackupSensor(stepSensor, currentMillis)

        // 같은 버킷 내 중복 저장 방지 기준점을 현재 버킷으로 설정.
        val currentUnitMillis = truncateToUnitMillis(currentMillis, getTimeZone())
        lastBackupUnitMillis = currentUnitMillis
    }

    /**
     * 30분 버킷 단위로 기준점을 저장한다.
     * 같은 버킷 내 중복 저장은 방지되어 있고, 버킷이 점프될 수 있다.
     */
    fun backupStepSensor(stepSensor: Long, currentMillis: Long) {
        val timeZone = getTimeZone()
        val currentUnitMillis = truncateToUnitMillis(currentMillis, timeZone)
        val currentUnitLdt = Instant.fromEpochMilliseconds(currentUnitMillis).toLocalDateTime(timeZone)

        val lastBackup = lastBackupUnitMillis
        if (lastBackup != null && currentUnitMillis <= lastBackup) return

        val saveMillis = lastBackupUnitMillis?.let { it + backupUnitMillis } ?: currentUnitMillis

        if (currentUnitLdt.hour == 0 && currentUnitLdt.minute == 0) {
            onDailyReset()
        } else {
            onBackupSensor(stepSensor, saveMillis)
        }

        lastBackupUnitMillis = currentUnitMillis
    }

    fun truncateToUnitMillis(millis: Long, timeZone: TimeZone): Long {
        val unitMinutes = (backupUnitMillis / 60_000L).toInt()
        val ldt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
        val truncatedMinute = (ldt.minute / unitMinutes) * unitMinutes
        val truncatedLdt = LocalDateTime(ldt.year, ldt.month, ldt.dayOfMonth, ldt.hour, truncatedMinute, 0, 0)
        return truncatedLdt.toInstant(timeZone).toEpochMilliseconds()
    }
}
