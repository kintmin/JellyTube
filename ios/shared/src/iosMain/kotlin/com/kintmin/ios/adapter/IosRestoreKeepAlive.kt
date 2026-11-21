package com.kintmin.ios.adapter

import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.RestoreKeepAlive

internal class IosRestoreKeepAlive : RestoreKeepAlive {
    override suspend fun <T> keepAlive(block: suspend () -> T): T = continueInBackgroundWhile(block)
}
