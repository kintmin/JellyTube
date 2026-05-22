package com.kintmin.platform.service

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class StepSensorProcessor(
    private val backupUnitMillis: Long,
    private val getZoneId: () -> ZoneId,
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
    fun forceBackupWithCurrentTime(stepSensor: Long, currentMillis: Long) {
        onBackupSensor(stepSensor, currentMillis)

        // 같은 버킷 내 중복 저장 방지 기준점을 현재 버킷으로 설정.
        val currentUnitMillis = truncateToUnitZonedDateTime(currentMillis, getZoneId()).toInstant().toEpochMilli()
        lastBackupUnitMillis = currentUnitMillis
    }

    /**
     * 30분 버킷 단위로 기준점을 저장한다.
     * 같은 버킷 내 중복 저장은 방지되어 있고, 버킷이 점프될 수 있다.
     *
     * - saveMillis: 정확한 버킷 시작 시각 (lastBackupUnitMillis + backupUnitMillis)
     *   lastBackupUnitMillis가 null인 경우(서비스 첫 시작 후 첫 버킷 변경)에는 currentUnitMillis 사용
     *
     * - newStepSensor가 아닌 prevStepSensor를 저장하는 이유:
     *   버킷 경계를 넘는 이벤트에서 newStepSensor를 저장하면 경계 순간의 걸음이 이전 시간대에 중복 귀속됨.
     *   prevStepSensor(경계 직전 값)가 다음 버킷의 시작 기준점으로 정확함.
     *
     * - onDailyReset() 은 다른 트리거로 자정 초기화가 안됐을 때 fallback 처리.
     */
    fun backupStepSensor(stepSensor: Long, currentMillis: Long) {
        val currentUnit = truncateToUnitZonedDateTime(currentMillis, getZoneId())
        val currentUnitMillis = currentUnit.toInstant().toEpochMilli()

        val lastBackup = lastBackupUnitMillis
        if (lastBackup != null && currentUnitMillis <= lastBackup) return

        val saveMillis = lastBackupUnitMillis?.let { it + backupUnitMillis } ?: currentUnitMillis

        if (currentUnit.hour == 0 && currentUnit.minute == 0) {
            onDailyReset()
        } else {
            onBackupSensor(stepSensor, saveMillis)
        }

        lastBackupUnitMillis = currentUnitMillis
    }

    fun truncateToUnitZonedDateTime(millis: Long, zoneId: ZoneId): ZonedDateTime {
        val unitMinutes = (backupUnitMillis / 60_000L).toInt()
        val zoned = Instant.ofEpochMilli(millis).atZone(zoneId)
        val truncatedMinute = (zoned.minute / unitMinutes) * unitMinutes

        return zoned
            .withMinute(truncatedMinute)
            .withSecond(0)
            .withNano(0)
    }
}
