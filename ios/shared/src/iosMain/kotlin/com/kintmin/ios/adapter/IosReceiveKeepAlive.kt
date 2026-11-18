package com.kintmin.ios.adapter

import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.presentation.platform.continueInBackgroundWhile

internal class IosReceiveKeepAlive : ReceiveKeepAlive {
    override suspend fun <T> keepAlive(block: suspend () -> T): T = continueInBackgroundWhile(block)
}
