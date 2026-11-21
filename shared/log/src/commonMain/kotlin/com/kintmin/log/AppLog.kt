package com.kintmin.log

import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.model.SentryEvent

interface AppLog {

    fun setLogConfig(userId: String)
    fun sendDebugLog(debugLog: DebugLog)
    fun sendErrorLog(errorLog: ErrorLog)
    fun sendSentryEvent(sentryEvent: SentryEvent)
}
