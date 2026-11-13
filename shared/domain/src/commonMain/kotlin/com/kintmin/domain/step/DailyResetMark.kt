package com.kintmin.domain.step

import kotlinx.atomicfu.atomic
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** 마지막으로 하루를 넘긴 날. 걸음을 세는 쪽이 하나를 들고 하루 초기화에 넘긴다. */
class DailyResetMark(initialEpochDay: Long) {

    // atomicfu 의 JVM 바이트코드 변환은 atomic 필드가 private 일 때만 적용된다.
    // private 가 아니면 kotlinx.atomicfu.* 참조가 남아 release(R8)에서 missing class 오류가 난다.
    private val epochDay = atomic(initialEpochDay)

    /** [today] 가 표시한 날보다 뒤면 오늘로 옮기고 표시해 둔 날을 돌려준다. 동시에 불려도 한 번만 돌려준다. */
    fun claim(today: Long): Long? {
        val previous = epochDay.value
        if (previous >= today) return null
        return if (epochDay.compareAndSet(previous, today)) previous else null
    }

    companion object {
        fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
            DailyResetMark(Clock.System.now().toLocalDateTime(timeZone).date.toEpochDays().toLong())
    }
}
