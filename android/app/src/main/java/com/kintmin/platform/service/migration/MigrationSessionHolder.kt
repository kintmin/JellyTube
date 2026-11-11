package com.kintmin.platform.service.migration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 서버(서비스)와 보내기 화면이 같은 세션을 봐야 해서 프로세스에 하나만 둔다. */
object MigrationSessionHolder {

    private val _session = MutableStateFlow<MigrationSession?>(null)
    val session: StateFlow<MigrationSession?> = _session.asStateFlow()

    /** 열려 있던 세션은 닫는다. 코드는 매번 새로 만든다. */
    fun open(): MigrationSession {
        _session.value?.close()
        return MigrationSession().also { _session.value = it }
    }

    fun close() {
        _session.value?.close()
        _session.value = null
    }
}
