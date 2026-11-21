package com.kintmin.domain.backup

import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupScheduleTest {

    private val seoul = TimeZone.of("Asia/Seoul")
    private fun millis(iso: String) = Instant.parse(iso).toEpochMilliseconds()

    @Test
    fun `쓴 적이 없으면 쓸 때다`() {
        assertTrue(BackupSchedule.isDue(null, millis("2026-11-21T03:00:00Z"), seoul))
    }

    @Test
    fun `같은 날 다시 부르면 쓰지 않는다`() {
        assertFalse(BackupSchedule.isDue(millis("2026-11-21T00:10:00+09:00"), millis("2026-11-21T23:50:00+09:00"), seoul))
    }

    @Test
    fun `날짜가 바뀌면 24시간이 안 됐어도 쓴다`() {
        assertTrue(BackupSchedule.isDue(millis("2026-11-20T23:30:00+09:00"), millis("2026-11-21T00:10:00+09:00"), seoul))
    }

    @Test
    fun `날짜는 넘긴 시간대로 가른다`() {
        val last = millis("2026-11-20T23:30:00+09:00")
        val now = millis("2026-11-21T00:10:00+09:00")
        assertFalse(BackupSchedule.isDue(last, now, TimeZone.UTC))
    }
}
