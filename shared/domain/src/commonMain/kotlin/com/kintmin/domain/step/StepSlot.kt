package com.kintmin.domain.step

/** 걸음수 차트 한 칸. 저장(Android 백업·iOS 기록)과 집계·화면이 같은 칸을 써야 값이 맞는다. */
object StepSlot {
    const val MINUTES = 30
    const val MILLIS = MINUTES * 60_000L
    const val PER_DAY = 24 * 60 / MINUTES

    fun indexOf(hour: Int, minute: Int): Int = (hour * 60 + minute) / MINUTES
}
