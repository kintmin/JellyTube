package com.kintmin.domain.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** 시스템 기본 타임존 기준 오늘 날짜를 반환한다. */
fun todayLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
    return Clock.System.now().toLocalDateTime(timeZone).date
}

/** 해당 날짜의 자정(00:00:00) epoch millis를 반환한다. */
fun LocalDate.startOfDayMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    return atStartOfDayIn(timeZone).toEpochMilliseconds()
}

/** BASIC_ISO_DATE(yyyyMMdd) 포맷으로 직렬화한다. */
fun LocalDate.toBasicIsoString(): String {
    return "$year${monthNumber.toString().padStart(2, '0')}${dayOfMonth.toString().padStart(2, '0')}"
}

/** BASIC_ISO_DATE(yyyyMMdd) 문자열을 LocalDate로 파싱한다. */
fun String.parseBasicIsoDate(): LocalDate {
    require(length == 8) { "Expected yyyyMMdd format, got: $this" }
    return LocalDate(
        year = substring(0, 4).toInt(),
        monthNumber = substring(4, 6).toInt(),
        dayOfMonth = substring(6, 8).toInt(),
    )
}
