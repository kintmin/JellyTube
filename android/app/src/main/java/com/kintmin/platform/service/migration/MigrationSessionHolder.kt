package com.kintmin.platform.service.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 서버(서비스)와 보내기 화면이 같은 세션을 봐야 해서 프로세스에 하나만 둔다. */
object MigrationSessionHolder {

    private val _session = MutableStateFlow<MigrationSession?>(null)
    val session: StateFlow<MigrationSession?> = _session.asStateFlow()

    // 화면이 첫 프레임부터 코드를 그리도록 세션과 같은 자리에서 동기로 바꾼다.
    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    /** 열려 있던 세션은 닫는다. 코드는 매번 새로 만든다. 코드를 여러 번 틀려 닫힌 세션은 비운다. */
    @Synchronized
    fun open(): MigrationSession {
        close()
        lateinit var opened: MigrationSession
        opened = MigrationSession(onClosed = { clearIfCurrent(opened) })
        _session.value = opened
        _pairingCode.value = opened.code
        return opened
    }

    @Synchronized
    fun close() {
        _session.value?.close()
    }

    @Synchronized
    private fun clearIfCurrent(session: MigrationSession) {
        if (_session.value !== session) return
        _session.value = null
        _pairingCode.value = null
    }
}
