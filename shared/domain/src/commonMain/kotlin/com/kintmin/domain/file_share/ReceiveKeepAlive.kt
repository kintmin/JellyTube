package com.kintmin.domain.file_share

/** 받는 도중 앱이 잠시 내려가도 OS 가 허락하는 만큼 [block] 을 이어서 돌린다. 프로세스가 살아 있는 플랫폼에서는 그냥 실행한다. */
interface ReceiveKeepAlive {
    suspend fun <T> keepAlive(block: suspend () -> T): T
}
