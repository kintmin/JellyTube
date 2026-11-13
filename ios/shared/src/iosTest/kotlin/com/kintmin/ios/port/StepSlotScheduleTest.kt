package com.kintmin.ios.port

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.usecase.CalculateStepCountUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepSlotScheduleTest {

    private val dayStart = 1_762_700_400_000L
    private val dayEnd = dayStart + StepSlotSchedule.DAY_MILLIS

    @Test
    fun `0시 1ms 뒤에 0 을 심는다`() {
        // 첫 칸은 시작과 끝 두 값이 다 있어야 차이를 낼 수 있다. 0시 정각은 앞날 마감의 자리다.
        val first = StepSlotSchedule.forDay(dayStart, nowMillis = dayStart + 1).first()

        assertEquals(dayStart + 1, first.rawCreatedTime)
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
                dayStart + 1,
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
    fun `적는 점은 모두 그날 다시 쓰는 구간 안에 있다`() {
        val range = StepSlotSchedule.ownedRange(dayStart)

        assertTrue(StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1).all { it.rawCreatedTime in range })
        assertTrue(dayStart !in range && dayEnd !in range)
    }

    @Test
    fun `iPhone 이 다시 쓴 첫날과 옮겨 온 앞날의 걸음수가 둘 다 맞다`() {
        val prevDayStart = dayStart - StepSlotSchedule.DAY_MILLIS
        // Android 는 30분 경계와 아무 시각(재부팅 기준점)에 센서 누적을 적고, 하루 마감을 다음 날 0시에 적는다.
        val migrated = listOf(
            StepData(prevDayStart, 50_000),
            StepData(prevDayStart + StepSlotSchedule.SLOT_MILLIS * 20, 53_000),
            StepData(dayStart, 56_000),
            StepData(dayStart + StepSlotSchedule.SLOT_MILLIS * 5 + 7, 56_400),
        )
        // 30분마다 10걸음씩 걸은 iPhone 의 하루 누적.
        val iphone = StepSlotSchedule.forDay(dayStart, nowMillis = dayEnd + 1).map { point ->
            val steps = point.queryUntilMillis?.let { (it - dayStart) / StepSlotSchedule.SLOT_MILLIS * 10 } ?: 0L
            StepData(point.rawCreatedTime, steps)
        }

        val range = StepSlotSchedule.ownedRange(dayStart)
        val stored = migrated.filterNot { it.rawCreatedTime in range } + iphone

        assertEquals(6_000, stepsOfDay(stored, prevDayStart))
        assertEquals(480, stepsOfDay(stored, dayStart))
    }

    /** 저장소와 같은 규칙: 그날 0시부터 다음 날 0시까지(양끝 포함)를 시각순으로 센다. */
    private fun stepsOfDay(stored: List<StepData>, dayStartMillis: Long): Int =
        CalculateStepCountUseCase()(
            stored.filter { it.rawCreatedTime in dayStartMillis..(dayStartMillis + StepSlotSchedule.DAY_MILLIS) }
                .sortedBy { it.rawCreatedTime }
                .map { it.stepSensor },
        )
}
