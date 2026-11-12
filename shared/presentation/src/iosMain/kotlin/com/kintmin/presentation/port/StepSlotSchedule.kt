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

    fun forDay(dayStartMillis: Long, nowMillis: Long): List<StepWritePoint> {
        if (dayStartMillis > nowMillis) return emptyList()

        val dayEndMillis = dayStartMillis + DAY_MILLIS
        val points = mutableListOf(StepWritePoint(dayStartMillis + 1, queryUntilMillis = null))
        boundaries(dayStartMillis, afterMillis = dayStartMillis, nowMillis = nowMillis)
            .mapTo(points) { StepWritePoint(it, queryUntilMillis = it) }

        if (dayEndMillis <= nowMillis) {
            points += StepWritePoint(dayEndMillis - 1, queryUntilMillis = dayEndMillis)
        }
        return points
    }

    /**
     * iPhone 이 통째로 다시 쓰는 그날의 구간. 한 날 안에 다른 기기의 기록이 섞이면 누적 기준이 달라
     * 계산이 깨지므로 지우고 다시 쓴다.
     *
     * 0시 정각은 뺀다. 그 자리는 앞날의 마감과 겹치는 칸이라(Android 는 전날 마지막 센서값을 0시에
     * 적는다) 지우면 앞날의 마지막 걸음이 사라진다. 그래서 0 은 0시 1ms 뒤에 심는다 — 0시 값보다
     * 작아져도 계산은 재부팅처럼 그 하락을 건너뛰고 0 부터 센다.
     */
    fun ownedRange(dayStartMillis: Long): LongRange =
        (dayStartMillis + 1) until (dayStartMillis + DAY_MILLIS)

    fun boundaries(dayStartMillis: Long, afterMillis: Long, nowMillis: Long): List<Long> {
        val dayEndMillis = dayStartMillis + DAY_MILLIS
        val result = mutableListOf<Long>()

        var boundaryMillis = afterMillis + SLOT_MILLIS
        while (boundaryMillis <= nowMillis && boundaryMillis < dayEndMillis) {
            result += boundaryMillis
            boundaryMillis += SLOT_MILLIS
        }
        return result
    }
}
