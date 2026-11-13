package com.kintmin.data.file_share.migration

import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 서버(서비스)와 보내기 화면이 같은 세션을 봐야 해서 프로세스에 하나만 둔다. */
object MigrationSessionHolder {

    private val _session = MutableStateFlow<MigrationSession?>(null)
    val session: StateFlow<MigrationSession?> = _session.asStateFlow()

    // 화면이 첫 프레임부터 상태를 그리도록 세션과 같은 자리에서 동기로 바꾼다.
    private val _state = MutableStateFlow<DataMigrationSendStatus>(DataMigrationSendStatus.Idle)
    val state: StateFlow<DataMigrationSendStatus> = _state.asStateFlow()

    /** 열려 있던 세션은 닫는다. 코드는 매번 새로 만든다. */
    @Synchronized
    fun open(): MigrationSession {
        // close() 는 Idle 을 거쳐 화면이 연결 끊김으로 깜빡인다.
        _session.value?.let { previous ->
            _session.value = null
            previous.close()
        }
        lateinit var opened: MigrationSession
        opened = MigrationSession(
            onClosed = { onClosed(opened) },
            onPaired = { updateIfCurrent(opened, DataMigrationSendStatus.Paired(it)) },
            onFinished = { updateIfCurrent(opened, DataMigrationSendStatus.Finished(it.newAudioMediaCount, it.failedCount)) },
        )
        _session.value = opened
        _state.value = DataMigrationSendStatus.WaitingForPair(opened.code)
        return opened
    }

    @Synchronized
    fun close() {
        val current = _session.value ?: return
        _session.value = null
        current.close()
        _state.value = DataMigrationSendStatus.Idle
    }

    @Synchronized
    private fun onClosed(session: MigrationSession) {
        if (_session.value !== session) return
        // close() 가 아니라 코드를 여러 번 틀려 닫혔다.
        _session.value = null
        _state.value = DataMigrationSendStatus.Locked
    }

    @Synchronized
    private fun updateIfCurrent(session: MigrationSession, state: DataMigrationSendStatus) {
        if (_session.value === session) _state.value = state
    }
}
