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
                Triple("DEBUG", "100% message", "tag"),
                Triple("ERROR", "failed / java.lang.Exception: boom", "tag"),
                Triple("SENTRY_EVENT", "DeleteAudioMedia {Source=s}", null),
            ),
            repository.lines,
        )
    }

    private class FakeAppLogRepository : AppLogRepository {
        val lines = mutableListOf<Triple<String, String, String?>>()

        override fun appendAppLog(type: String, message: String, tag: String?) {
            lines += Triple(type, message, tag)
        }

        override suspend fun fetchAppLogDateList() = Result.success(emptyList<String>())
        override suspend fun fetchAppLogLineList(date: String) = Result.success(emptyList<String>())
        override suspend fun fetchAppLogFileFullPaths() = Result.success(emptyList<String>())
    }
}
