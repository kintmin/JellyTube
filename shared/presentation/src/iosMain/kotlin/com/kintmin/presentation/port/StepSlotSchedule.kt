package com.kintmin.presentation.port

/** @param queryUntilMillis 그날 0시부터 이 시각까지의 누적을 적는다. null 이면 0 을 적는다. */
internal data class StepWritePoint(
    val rawCreatedTime: Long,
    val queryUntilMillis: Long?,
)

/**
 * iOS 가 `STEP` 에 걸음수를 적는 시점과 값.
 *
 * 화면은 이웃한 두 값의 차이만 쓰므로(`CalculateStepCountUseCase`) 하루 안에서 단조증가이면
 * 된다. 여기서는 `CMPedometer` 가 주는 그대로 그날 0시부터의 누적을 쓴다.
 */
internal object StepSlotSchedule {

    /** 차트 한 칸. `GetHalfHourlyStepsUseCase` 가 하루를 48칸으로 나눈다. */
    const val SLOT_MILLIS = 30L * 60L * 1000L
    const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    /** @param storedUntilMillis 이 시각까지는 이미 적혀 있다. 그 뒤의 점만 준다 — 옮겨 온 기록을 덮지 않는다. */
    fun forDay(dayStartMillis: Long, nowMillis: Long, storedUntilMillis: Long? = null): List<StepWritePoint> {
        if (dayStartMillis > nowMillis) return emptyList()

        val dayEndMillis = dayStartMillis + DAY_MILLIS
        val points = mutableListOf(StepWritePoint(dayStartMillis, queryUntilMillis = null))
        boundaries(dayStartMillis, afterMillis = dayStartMillis, nowMillis = nowMillis)
            .mapTo(points) { StepWritePoint(it, queryUntilMillis = it) }

        if (dayEndMillis <= nowMillis) {
            points += StepWritePoint(dayEndMillis - 1, queryUntilMillis = dayEndMillis)
        }
        return points.filter { storedUntilMillis == null || it.rawCreatedTime > storedUntilMillis }
    }

    fun boundaries(dayStartMillis: Long, afterMillis: Long, nowMillis: Long): List<Long> {
        val dayEndMillis = dayStartMillis + DAY_MILLIS
        val result = mutableListOf<Long>()

        // afterMillis 가 칸 경계가 아닐 수 있다(옮겨 온 기록). 그 뒤 첫 경계부터 준다.
        val slotsPassed = (maxOf(afterMillis, dayStartMillis) - dayStartMillis) / SLOT_MILLIS
        var boundaryMillis = dayStartMillis + (slotsPassed + 1) * SLOT_MILLIS
        while (boundaryMillis <= nowMillis && boundaryMillis < dayEndMillis) {
            result += boundaryMillis
            boundaryMillis += SLOT_MILLIS
        }
        return result
    }
}
