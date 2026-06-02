package com.kintmin.domain.extension

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun LocalDateTime.toMillis(): Long {
    return toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
