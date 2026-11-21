package com.kintmin.data.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class SentryAppLogTest {

    private val repository = FakeAppLogRepository()
    private val appLog = SentryAppLog(repository)

    @Test
    fun `보낸 것은 파일 로그에도 남는다`() {
        appLog.sendDebugLog(DebugLog("tag", "100% message"))
        appLog.sendErrorLog(ErrorLog("tag", "failed", Exception("boom")))
        appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(source = "s"))

        assertEquals(
            listOf(
                "DEBUG" to "tag: 100% message",
                "ERROR" to "tag: failed / java.lang.Exception: boom",
                "SENTRY_EVENT" to "DeleteAudioMedia {Source=s}",
            ),
            repository.lines,
        )
    }

    private class FakeAppLogRepository : AppLogRepository {
        val lines = mutableListOf<Pair<String, String>>()

        override fun appendAppLog(type: String, message: String) {
            lines += type to message
        }

        override suspend fun fetchAppLogDateList() = Result.success(emptyList<String>())
        override suspend fun fetchAppLogLineList(date: String) = Result.success(emptyList<String>())
        override suspend fun fetchAppLogFileFullPaths() = Result.success(emptyList<String>())
    }
}
