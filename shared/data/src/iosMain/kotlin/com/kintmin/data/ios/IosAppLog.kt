package com.kintmin.data.ios

import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.FirebaseEvent

internal class IosAppLog : AppLog {
    override fun setLogConfig(userId: String) = Unit

    override fun sendDebugLog(debugLog: DebugLog) = Unit

    override fun sendFirebaseEvent(firebaseEvent: FirebaseEvent) = Unit
}
