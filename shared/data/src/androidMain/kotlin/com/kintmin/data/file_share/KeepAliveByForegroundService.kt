package com.kintmin.data.file_share

import com.kintmin.domain.file_share.ReceiveKeepAlive

/** 포그라운드 서비스가 프로세스를 붙잡아 두므로 따로 유예를 청하지 않는다. */
internal object KeepAliveByForegroundService : ReceiveKeepAlive {
    override suspend fun <T> keepAlive(block: suspend () -> T): T = block()
}
