package com.kintmin.ios.adapter

import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.MigrationReceiveKeepAlive

internal class IosMigrationReceiveKeepAlive : MigrationReceiveKeepAlive {
    override suspend fun <T> keepAlive(block: suspend () -> T): T = continueInBackgroundWhile(block)
}
