package com.kintmin.ios.adapter

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.ios.bridge.IosAppLogBridge
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.SentryEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosAppLogTest {

    private val bridge = FakeAppLogBridge()
    private val repository = FakeAppLogRepository()
    private val appLog = IosAppLog(bridge, repository)

    @Test
    fun `값이 없는 param 은 보내지 않는다`() {
        // Android 의 bundleOf 도 null 을 싣지 않는다. 두 플랫폼이 같은 모양을 보내야 한다.
        appLog.sendSentryEvent(
            SentryEvent.FailedDownloadAudioMedia(source = "s", exception = Exception("boom")),
        )

        assertEquals(setOf("Source", "ErrorMessage"), bridge.events.single().second.keys)
    }

    @Test
    fun `보낸 것은 파일 로그에도 남는다`() {
        appLog.sendDebugLog(DebugLog("tag", "message"))
        appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(source = "s"))

        assertEquals(listOf("DEBUG", "FIREBASE_EVENT"), repository.lines.map { it.first })
        assertTrue(repository.lines.first().second.startsWith("tag: message"))
    }

    private class FakeAppLogBridge : IosAppLogBridge {
        val userIds = mutableListOf<String>()
        val events = mutableListOf<Pair<String, Map<String, String>>>()

        override fun setUserId(userId: String) {
            userIds += userId
        }

        override fun debug(tag: String, message: String) = Unit

        override fun event(name: String, params: Map<String, String>) {
            events += name to params
        }
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
