package com.kintmin.domain.step

/** 걸음수 차트 한 칸. */
object StepSlot {
    const val MINUTES = 30
    const val MILLIS = MINUTES * 60_000L
    const val PER_DAY = 24 * 60 / MINUTES

    fun indexOf(hour: Int, minute: Int): Int = (hour * 60 + minute) / MINUTES
}
