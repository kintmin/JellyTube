package com.kintmin.presentation.port

import kotlinx.coroutines.flow.StateFlow

/** PC에서 파일을 받는 수신 서버를 켜고 끈다. */
interface FileShareServerController {

    val isRunning: StateFlow<Boolean>

    fun start()

    fun stop()
}
