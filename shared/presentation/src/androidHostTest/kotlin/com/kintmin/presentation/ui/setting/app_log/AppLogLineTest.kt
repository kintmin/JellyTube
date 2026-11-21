package com.kintmin.presentation.ui.setting.app_log

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLogLineTest {

    @Test
    fun `시각과 타입과 메시지로 나눈다`() {
        assertEquals(
            AppLogLineUiState(time = "09:05:01.042", type = "SENTRY_EVENT", message = "download [id=3] 완료"),
            parseAppLogLine("[2026-11-21 09:05:01.042] [SENTRY_EVENT] download [id=3] 완료"),
        )
    }

    @Test
    fun `타입이 없으면 시각과 메시지만 나눈다`() {
        assertEquals(
            AppLogLineUiState(time = "23:59:59.999", type = null, message = "메시지"),
            parseAppLogLine("[2026-11-21 23:59:59.999] 메시지"),
        )
    }

    @Test
    fun `형식을 벗어난 줄은 원문을 메시지로 둔다`() {
        val line = "    at com.kintmin.Foo.bar(Foo.kt:12)"

        assertEquals(AppLogLineUiState(time = null, type = null, message = line), parseAppLogLine(line))
    }
}
