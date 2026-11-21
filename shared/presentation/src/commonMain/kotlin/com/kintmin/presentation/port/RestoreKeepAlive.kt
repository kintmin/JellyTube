package com.kintmin.presentation.port

/** 백업을 복원하는 동안 앱이 내려가도 [keepAlive] 의 `block` 이 이어지게 OS 가 허락하는 장치를 건다. */
interface RestoreKeepAlive {

    suspend fun <T> keepAlive(block: suspend () -> T): T
}
