package com.kintmin.domain.common.extension

import java.time.Instant
import java.time.LocalDateTime

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this).toLocalDateTime()
}