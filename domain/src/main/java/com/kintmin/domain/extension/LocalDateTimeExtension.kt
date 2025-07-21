package com.kintmin.domain.extension

import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDateTime.toMillis(): Long {
    return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
