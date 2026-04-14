package com.kintmin.log

import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent

interface AppLog {

    fun setLogConfig(userId: String)
    fun sendDebugLog(debugLog: DebugLog)
    fun sendFirebaseEvent(firebaseEvent: FirebaseEvent)
}
