package com.kintmin.data.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.secret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SentryAppLogTest {

    private val repository = FakeAppLogRepository()
    private val secretMode = FakeSecretModeRepository()
    private val appLog = SentryAppLog(repository, secretMode)

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

    @Test
    fun `비밀모드면 표시한 값을 메시지와 이벤트 속성에서 가린다`() {
        secretMode.isEnabledFlow.value = true

        appLog.sendDebugLog(DebugLog("tag", "name=${secret("곡")}"))
        appLog.sendErrorLog(ErrorLog("tag", "name=${secret("곡")}", Exception("boom")))
        appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(source = secret("https://y/1")))

        assertEquals(
            listOf(
                Triple("DEBUG", "name=<비밀>", "tag"),
                Triple("ERROR", "name=<비밀> / java.lang.Exception: boom", "tag"),
                Triple("SENTRY_EVENT", "DeleteAudioMedia {Source=<비밀>}", null),
            ),
            repository.lines,
        )
    }

    @Test
    fun `비밀모드가 아니면 표시만 걷어 원문을 남긴다`() {
        appLog.sendDebugLog(DebugLog("tag", "name=${secret("곡")}"))
        appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(source = secret("https://y/1")))

        assertEquals(
            listOf(
                Triple("DEBUG", "name=곡", "tag"),
                Triple("SENTRY_EVENT", "DeleteAudioMedia {Source=https://y/1}", null),
            ),
            repository.lines,
        )
    }

    private class FakeSecretModeRepository : SecretModeRepository {
        override val isEnabledFlow = MutableStateFlow(false)
        override suspend fun setEnabled(value: Boolean) {
            isEnabledFlow.value = value
        }

        override fun getMinutesFlow() = emptyFlow<Int>()
        override suspend fun updateMinutes(minutes: Int) = Result.success(Unit)
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
