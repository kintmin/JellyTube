package com.kintmin.platform.service

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StepSensorProcessorTest {

    private val utc: TimeZone = TimeZone.UTC
    private val backupUnit = 30 * 60 * 1000L

    private val stepDeltaCalls = mutableListOf<Int>()
    private val forceBackupCalls = mutableListOf<Pair<Long, Long>>()
    private val backupSensorCalls = mutableListOf<Pair<Long, Long>>()
    private var dailyResetCount = 0

    private lateinit var processor: StepSensorProcessor

    @Before
    fun setUp() {
        stepDeltaCalls.clear()
        forceBackupCalls.clear()
        backupSensorCalls.clear()
        dailyResetCount = 0
        processor = StepSensorProcessor(
            backupUnitMillis = backupUnit,
            getTimeZone = { utc },
            onBackupSensor = { sensor, saveMillis ->
                val truncated = processor.truncateToUnitMillis(saveMillis, utc)
                if (truncated == saveMillis) {
                    backupSensorCalls.add(sensor to saveMillis)
                } else {
                    forceBackupCalls.add(sensor to saveMillis)
                }
            },
            onDailyReset = { dailyResetCount++ },
        )
    }

    private fun utcMillis(hour: Int, minute: Int, second: Int = 0): Long =
        LocalDateTime(2024, 1, 15, hour, minute, second, 0).toInstant(utc).toEpochMilliseconds()

    private fun ldt(millis: Long): kotlinx.datetime.LocalDateTime =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(utc)

    // ─── truncateToUnitMillis ────────────────────────────────────────────────

    @Test
    fun `00시 00분 00초는 00시 00분 버킷에 속한다`() {
        val result = ldt(processor.truncateToUnitMillis(utcMillis(0, 0, 0), utc))
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
        assertEquals(0, result.second)
    }

    @Test
    fun `00시 29분 59초는 경계 직전으로 00시 00분 버킷에 속한다`() {
        val result = ldt(processor.truncateToUnitMillis(utcMillis(0, 29, 59), utc))
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `00시 30분 00초는 00시 30분 버킷 정각이다`() {
        val result = ldt(processor.truncateToUnitMillis(utcMillis(0, 30, 0), utc))
        assertEquals(0, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `23시 59분 59초는 23시 30분 버킷에 속한다`() {
        val result = ldt(processor.truncateToUnitMillis(utcMillis(23, 59, 59), utc))
        assertEquals(23, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `truncate 결과��� 초와 나노초는 항상 0이다`() {
        val result = ldt(processor.truncateToUnitMillis(utcMillis(9, 47, 33), utc))
        assertEquals(0, result.second)
        assertEquals(0, result.nanosecond)
    }

    // ─── updateStep: 첫 이벤트 및 재부팅 감지 ────────────────────────────

    @Test
    fun `prevSensor가 null이면(hasNoPrevSensor) forceBackup이 호출된다`() {
        processor.updateStep(null, 100L, utcMillis(9, 15)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(1, forceBackupCalls.size)
        assertEquals(0, stepDeltaCalls.size)
    }

    // ─── updateStep: lastSavedStepSensor delta 복원 ──────────────────────

    @Test
    fun `재시작 케이스 - newSensor가 lastSaved 이상이면 차이만큼 delta가 호출된다`() {
        processor.updateStep(null, 5350L, utcMillis(9, 15), todayLastSavedStepSensor = 5300L) {
            stepDeltaCalls.add(it)
        }

        assertEquals(listOf(50), stepDeltaCalls)
        assertEquals(1, forceBackupCalls.size)
    }

    @Test
    fun `재부팅 케이스 - newSensor가 lastSaved 미만이면 newSensor 자체가 delta로 호출된다`() {
        processor.updateStep(null, 100L, utcMillis(9, 15), todayLastSavedStepSensor = 5300L) {
            stepDeltaCalls.add(it)
        }

        assertEquals(listOf(100), stepDeltaCalls)
        assertEquals(1, forceBackupCalls.size)
    }

    @Test
    fun `lastSavedStepSensor가 null이면 delta 복원 없이 forceBackup만 호출된다`() {
        processor.updateStep(null, 5350L, utcMillis(9, 15), todayLastSavedStepSensor = null) {
            stepDeltaCalls.add(it)
        }

        assertEquals(0, stepDeltaCalls.size)
        assertEquals(1, forceBackupCalls.size)
    }

    @Test
    fun `newSensor와 lastSaved가 같으면 delta가 0이라 stepDelta는 호출되지 않는다`() {
        processor.updateStep(null, 5300L, utcMillis(9, 15), todayLastSavedStepSensor = 5300L) {
            stepDeltaCalls.add(it)
        }

        assertEquals(0, stepDeltaCalls.size)
        assertEquals(1, forceBackupCalls.size)
    }

    @Test
    fun `prevSensor가 null이 아니면 lastSavedStepSensor가 있어도 delta 복원이 적용되지 않는다`() {
        processor.updateStep(5000L, 5350L, utcMillis(9, 15), todayLastSavedStepSensor = 5300L) {
            stepDeltaCalls.add(it)
        }

        // 일반 delta(5350 - 5000 = 350)만 호출, forceBackup 없음
        assertEquals(listOf(350), stepDeltaCalls)
        assertEquals(0, forceBackupCalls.size)
    }

    @Test
    fun `정상 이벤트에서 stepDelta가 누적된다`() {
        processor.updateStep(100L, 250L, utcMillis(9, 5)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(listOf(150), stepDeltaCalls)
        assertEquals(0, forceBackupCalls.size)
    }

    // ─── backupStepSensor: 버킷 중복 방지 ───────────────────────────────

    @Test
    fun `같은 버킷 내 두 번 이벤트는 onBackupSensor가 1회만 호출된다`() {
        processor.updateStep(100L, 110L, utcMillis(9, 5)) {
            stepDeltaCalls.add(it)
        }
        processor.updateStep(110L, 120L, utcMillis(9, 10)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(1, backupSensorCalls.size)
    }

    @Test
    fun `버킷이 바뀔 때마다 onBackupSensor가 호출된다`() {
        processor.updateStep(100L, 110L, utcMillis(9, 5)) {
            stepDeltaCalls.add(it)
        }
        processor.updateStep(110L, 120L, utcMillis(9, 35)) {
            stepDeltaCalls.add(it)
        }
        processor.updateStep(120L, 130L, utcMillis(10, 5)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(3, backupSensorCalls.size)
    }

    @Test
    fun `lastBackupUnitMillis가 null일 때 첫 버킷 변경 시 saveMillis는 currentUnitMillis다`() {
        val eventMillis = utcMillis(9, 35)
        processor.updateStep(100L, 120L, eventMillis) {
            stepDeltaCalls.add(it)
        }

        val expectedSaveMillis = processor.truncateToUnitMillis(eventMillis, utc)
        assertEquals(expectedSaveMillis, backupSensorCalls.first().second)
    }

    @Test
    fun `backupStepSensor에 저장되는 값은 prevStepSensor다`() {
        processor.updateStep(100L, 150L, utcMillis(9, 5)) {
            stepDeltaCalls.add(it)
        }
        processor.updateStep(150L, 200L, utcMillis(9, 35)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(150L, backupSensorCalls.last().first)
    }

    @Test
    fun `saveMillis는 lastBackupUnitMillis에 backupUnit을 더한 값이다`() {
        processor.updateStep(100L, 110L, utcMillis(9, 5)) { // lastBackupUnitMillis = 09:00
            stepDeltaCalls.add(it)
        }
        processor.updateStep(110L, 120L, utcMillis(9, 35)) { // saveMillis = 09:00 + 30min = 09:30
            stepDeltaCalls.add(it)
        }

        val expected = processor.truncateToUnitMillis(utcMillis(9, 0), utc) + backupUnit
        assertEquals(expected, backupSensorCalls.last().second)
    }

    // ─── forceBackup 후 버킷 skip ───────────────────────────────────────

    @Test
    fun `forceBackup 직후 같은 버킷 이벤트에서 onBackupSensor가 호출되지 않는다`() {
        processor.updateStep(null, 50L, utcMillis(9, 15)) { // forceBackup, lastBackupUnitMillis = 09:30
            stepDeltaCalls.add(it)
        }

        processor.updateStep(50L, 80L, utcMillis(9, 20)) { // 09:30 버킷 → skip
            stepDeltaCalls.add(it)
        }

        assertEquals(0, backupSensorCalls.size)
    }

    @Test
    fun `forceBackup 직후 다음 버킷 이벤트에서 onBackupSensor가 호출되고 prevStepSensor가 저장된다`() {
        processor.updateStep(null, 50L, utcMillis(9, 15)) { // forceBackup, lastBackupUnitMillis=09:00
            stepDeltaCalls.add(it)
        }
        processor.updateStep(50L, 100L, utcMillis(9, 32)) { // 09:30 버킷 → 저장, prevSensor=50
            stepDeltaCalls.add(it)
        }
        processor.updateStep(100L, 150L, utcMillis(10, 2)) { // 10:00 버킷 → 저장, prevSensor=100
            stepDeltaCalls.add(it)
        }

        assertEquals(2, backupSensorCalls.size)
        assertEquals(50L, backupSensorCalls[0].first)
        assertEquals(100L, backupSensorCalls[1].first)
    }

    // ─── 자정 경계 ───────────────────────────────────────────────────────

    @Test
    fun `00시 00분~29분 버킷 이벤트에서 onDailyReset이 호출되고 onBackupSensor는 호출되지 않는다`() {
        processor.backupStepSensor(stepSensor = 1000L, currentMillis = utcMillis(0, 15))

        assertEquals(1, dailyResetCount)
        assertEquals(0, backupSensorCalls.size)
    }

    @Test
    fun `서비스 재시작 시 자정 버킷이어도 onDailyReset은 호출되지 않는다`() {
        // ResetDataOncePerDayUseCase.cachedEpochDay는 프로세스 시작 시 오늘로 초기화되므로 항상 no-op
        processor.updateStep(null, 50L, utcMillis(0, 15)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(0, dailyResetCount)
        assertEquals(1, forceBackupCalls.size)
    }

    @Test
    fun `forceBackupWithCurrentTime을 자정 버킷 시각으로 직접 호출해도 onDailyReset은 호출되지 않는다`() {
        processor.forceBackupWithCurrentTime(stepSensor = 1000L, currentMillis = utcMillis(0, 15))

        assertEquals(0, dailyResetCount)
    }

    // ─── 서비스 재시작 (재부팅 없음) ────────────────────────────────────

    @Test
    fun `서비스 재시작 후 첫 이벤트는 prevSensor가 null(날��� 불일치 포함)이므로 forceBackup이 호출된다`() {
        processor.updateStep(null, 2000L, utcMillis(9, 5)) {
            stepDeltaCalls.add(it)
        }

        assertEquals(1, forceBackupCalls.size)
        assertEquals(0, stepDeltaCalls.size)
    }
}
