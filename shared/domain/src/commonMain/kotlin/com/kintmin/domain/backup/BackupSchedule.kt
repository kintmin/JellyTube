package com.kintmin.domain.backup

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object BackupSchedule {

    fun isDue(lastSuccessMillis: Long?, nowMillis: Long, timeZone: TimeZone): Boolean {
        if (lastSuccessMillis == null) return true
        return dateOf(lastSuccessMillis, timeZone) != dateOf(nowMillis, timeZone)
    }

    private fun dateOf(millis: Long, timeZone: TimeZone) =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
}
