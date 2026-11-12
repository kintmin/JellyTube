package com.kintmin.presentation.port

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepSlotScheduleTest {

    private val dayStart = 1_762_700_400_000L
    private val dayEnd = dayStart + StepSlotSchedule.DAY_MILLIS

    @Test
    fun `0시에 0 을 심는다`() {
        // 첫 칸은 시작과 끝 두 값이 다 있어야 차이를 낼 수 있다.
        val first = StepSlotSchedule.forDay(dayStart, nowMillis = dayStart + 1).first()

        assertEquals(dayStart, first.rawCreatedTime)
        assertEquals(null, first.queryUntilMillis)
    }

    @Test
    fun `지나간 날의 마감은 자정 1ms 전에 적는다`() {
        // 하루 기준 누적은 자정에 0 으로 떨어진다. 자정에 적으면 다음 날 첫 칸과 겹쳐 하나가 깨진다.
        val last = StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1).last()

        assertEquals(dayEnd - 1, last.rawCreatedTime)
        assertEquals(dayEnd, last.queryUntilMillis)
    }

    @Test
    fun `지나간 하루는 0시 anchor 와 48칸 경계로 채워진다`() {
        val points = StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1)

        assertEquals(1 + 47 + 1, points.size)
        assertTrue(points.map { it.rawCreatedTime }.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `아직 오지 않은 날은 아무것도 적지 않는다`() {
        assertEquals(emptyList(), StepSlotSchedule.forDay(dayStart, nowMillis = dayStart - 1))
    }

    @Test
    fun `오늘은 지나간 경계까지만 적고 마감하지 않는다`() {
        val now = dayStart + StepSlotSchedule.SLOT_MILLIS * 3 + 1
        val points = StepSlotSchedule.forDay(dayStart, nowMillis = now)

        assertEquals(
            listOf(
                dayStart,
                dayStart + StepSlotSchedule.SLOT_MILLIS,
                dayStart + StepSlotSchedule.SLOT_MILLIS * 2,
                dayStart + StepSlotSchedule.SLOT_MILLIS * 3,
            ),
            points.map { it.rawCreatedTime },
        )
    }

    @Test
    fun `경계 목록은 마지막으로 적은 지점 다음부터 준다`() {
        val lastWritten = dayStart + StepSlotSchedule.SLOT_MILLIS * 2
        val now = lastWritten + StepSlotSchedule.SLOT_MILLIS * 2

        assertEquals(
            listOf(lastWritten + StepSlotSchedule.SLOT_MILLIS, now),
            StepSlotSchedule.boundaries(dayStart, afterMillis = lastWritten, nowMillis = now),
        )
    }

    @Test
    fun `경계 목록은 그날을 넘지 않는다`() {
        val boundaries = StepSlotSchedule.boundaries(
            dayStartMillis = dayStart,
            afterMillis = dayEnd - StepSlotSchedule.SLOT_MILLIS * 2,
            nowMillis = dayEnd + StepSlotSchedule.DAY_MILLIS,
        )

        assertEquals(listOf(dayEnd - StepSlotSchedule.SLOT_MILLIS), boundaries)
    }

    @Test
    fun `이미 적힌 시각까지는 다시 적지 않는다`() {
        val stored = dayStart + StepSlotSchedule.SLOT_MILLIS * 2
        val points = StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1, storedUntilMillis = stored)

        assertEquals(stored + StepSlotSchedule.SLOT_MILLIS, points.first().rawCreatedTime)
        assertEquals(dayEnd - 1, points.last().rawCreatedTime)
    }

    @Test
    fun `적힌 기록이 그날을 지났으면 그날은 아무것도 적지 않는다`() {
        val points = StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1, storedUntilMillis = dayEnd + 1)

        assertEquals(emptyList(), points)
    }

    @Test
    fun `경계 목록은 칸 중간에서 시작해도 칸 경계에 맞춘다`() {
        val now = dayStart + StepSlotSchedule.SLOT_MILLIS * 3

        assertEquals(
            listOf(dayStart + StepSlotSchedule.SLOT_MILLIS * 2, now),
            StepSlotSchedule.boundaries(
                dayStartMillis = dayStart,
                afterMillis = dayStart + StepSlotSchedule.SLOT_MILLIS + 7,
                nowMillis = now,
            ),
        )
    }
}
