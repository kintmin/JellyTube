package com.kintmin.domain.common.extension

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(): LocalDateTime {
    return atZone(ZoneId.systemDefault()).toLocalDateTime()
}