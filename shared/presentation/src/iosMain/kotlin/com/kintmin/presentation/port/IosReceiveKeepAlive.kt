package com.kintmin.presentation.port

import com.kintmin.domain.file_share.ReceiveKeepAlive

internal class IosReceiveKeepAlive : ReceiveKeepAlive {
    override suspend fun <T> keepAlive(block: suspend () -> T): T = withBackgroundExecutionWindow(WINDOW_NAME) { block() }

    private companion object {
        const val WINDOW_NAME = "pc-upload"
    }
}
