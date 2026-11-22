package com.kintmin.data.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.domain.secret_mode.repository.SecretModeRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.revealSecrets
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import io.sentry.kotlin.multiplatform.protocol.User

internal class SentryAppLog(
    private val appLogRepository: AppLogRepository,
    private val secretModeRepository: SecretModeRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        Sentry.setUser(User(id = userId))
        appLogRepository.appendAppLog("SENTRY_CONFIG", "setUserId=$userId")
        // 크래시는 수집기만 잡아 앱 로그에는 흔적이 없다. 다음 실행에서 남긴다.
        if (Sentry.isCrashedLastRun()) appLogRepository.appendAppLog("CRASH", "지난 실행이 비정상 종료됨")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        val message = reveal(debugLog.message)
        val line = "${debugLog.tag}: $message"
        println(line)
        // 메시지를 템플릿으로 넘기면 그 안의 '%' 가 자리표시자로 읽힌다.
        Sentry.logger.debug("%s", message) { this["tag"] = debugLog.tag }
        Sentry.addBreadcrumb(Breadcrumb.debug(message).apply { category = debugLog.tag })
        appLogRepository.appendAppLog("DEBUG", line)
    }

    override fun sendErrorLog(errorLog: ErrorLog) {
        val message = reveal(errorLog.message)
        val line = "${errorLog.tag}: $message" + (errorLog.throwable?.let { " / $it" } ?: "")
        println(line)
        Sentry.logger.error("%s", line) { this["tag"] = errorLog.tag }
        capture(errorLog.tag, message, errorLog.throwable)
        appLogRepository.appendAppLog("ERROR", line)
    }

    override fun sendSentryEvent(sentryEvent: SentryEvent) {
        val name = sentryEvent.logName.name
        val attributes = sentryEvent.attributes.mapValues { reveal(it.value) }
        val line = "$name {${attributes.entries.joinToString(", ") { "${it.key}=${it.value}" }}}"
        println(line)
        Sentry.logger.info(name) { attributes.forEach { (key, value) -> this[key] = value } }
        sentryEvent.throwable?.let { capture(name, line, it) }
            ?: Sentry.addBreadcrumb(Breadcrumb.info(line).apply { category = "event" })
        appLogRepository.appendAppLog("SENTRY_EVENT", line)
    }

    private fun reveal(text: String) = revealSecrets(text, hide = secretModeRepository.isEnabledFlow.value)

    private fun capture(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Sentry.captureException(throwable) {
                it.setTag("tag", tag)
                it.setExtra("message", message)
            }
        } else {
            Sentry.captureMessage("$tag: $message") {
                it.level = SentryLevel.ERROR
                it.setTag("tag", tag)
            }
        }
    }
}
