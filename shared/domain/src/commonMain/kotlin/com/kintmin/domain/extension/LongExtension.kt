package com.kintmin.domain.extension

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
}
