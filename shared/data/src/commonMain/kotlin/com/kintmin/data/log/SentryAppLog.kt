package com.kintmin.data.log

import com.kintmin.domain.app_log.repository.AppLogRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import io.sentry.kotlin.multiplatform.protocol.User

internal class SentryAppLog(
    private val appLogRepository: AppLogRepository,
) : AppLog {

    override fun setLogConfig(userId: String) {
        Sentry.setUser(User(id = userId))
        appLogRepository.appendAppLog("SENTRY_CONFIG", "setUserId=$userId")
    }

    override fun sendDebugLog(debugLog: DebugLog) {
        val line = "${debugLog.tag}: ${debugLog.message}"
        println(line)
        // 메시지를 템플릿으로 넘기면 그 안의 '%' 가 자리표시자로 읽힌다.
        Sentry.logger.debug("%s", debugLog.message) { this["tag"] = debugLog.tag }
        Sentry.addBreadcrumb(Breadcrumb.debug(debugLog.message).apply { category = debugLog.tag })
        appLogRepository.appendAppLog("DEBUG", line)
    }

    override fun sendErrorLog(errorLog: ErrorLog) {
        val line = "${errorLog.tag}: ${errorLog.message}" + (errorLog.throwable?.let { " / $it" } ?: "")
        println(line)
        Sentry.logger.error("%s", line) { this["tag"] = errorLog.tag }
        capture(errorLog.tag, errorLog.message, errorLog.throwable)
        appLogRepository.appendAppLog("ERROR", line)
    }

    override fun sendSentryEvent(sentryEvent: SentryEvent) {
        val name = sentryEvent.logName.name
        val attributes = sentryEvent.attributes
        val line = "$name {${attributes.entries.joinToString(", ") { "${it.key}=${it.value}" }}}"
        println(line)
        Sentry.logger.info(name) { attributes.forEach { (key, value) -> this[key] = value } }
        sentryEvent.throwable?.let { capture(name, line, it) }
            ?: Sentry.addBreadcrumb(Breadcrumb.info(line).apply { category = "event" })
        appLogRepository.appendAppLog("SENTRY_EVENT", line)
    }

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
