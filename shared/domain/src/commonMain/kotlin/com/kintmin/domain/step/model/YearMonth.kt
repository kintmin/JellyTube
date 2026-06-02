package com.kintmin.domain.step.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * java.time.YearMonth 대체 KMP 공용 클래스.
 */
data class YearMonth(val year: Int, val month: Month) {

    constructor(year: Int, monthNumber: Int) : this(year, Month(monthNumber))

    val monthNumber: Int get() = month.ordinal + 1

    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)

    val atEndOfMonth: LocalDate
        get() {
            val nextYear = if (monthNumber == 12) year + 1 else year
            val nextMonth = if (monthNumber == 12) 1 else monthNumber + 1
            return LocalDate(nextYear, nextMonth, 1).minus(1, DateTimeUnit.DAY)
        }

    val lengthOfMonth: Int get() = atEndOfMonth.dayOfMonth

    fun plusMonths(count: Int): YearMonth {
        val totalMonths = (monthNumber - 1) + count
        val newYear = year + totalMonths.floorDiv(12)
        val newMonth = totalMonths.mod(12) + 1
        return YearMonth(newYear, newMonth)
    }

    fun minusMonths(count: Int): YearMonth = plusMonths(-count)

    companion object {
        fun now(timeZone: TimeZone = TimeZone.currentSystemDefault()): YearMonth {
            val d = Clock.System.now().toLocalDateTime(timeZone)
            return YearMonth(d.year, d.month)
        }

        fun from(date: LocalDate): YearMonth = YearMonth(date.year, date.month)
    }
}
