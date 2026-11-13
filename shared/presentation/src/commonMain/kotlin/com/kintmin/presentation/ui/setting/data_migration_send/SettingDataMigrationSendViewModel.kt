package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.RunningWorkMonitor
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이 화면이 떠 있는 동안만 이전 엔드포인트가 열린다. */
class SettingDataMigrationSendViewModel constructor(
    private val dataMigrationSendController: DataMigrationSendController,
    private val runningWorkMonitor: RunningWorkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationSendUiState())
    val uiState: StateFlow<SettingDataMigrationSendUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    init {
        startIfIdle()
        viewModelScope.launch {
            dataMigrationSendController.status.collect { status ->
                if (hasStarted) _uiState.update { it.copy(step = status.toStep()) }
            }
        }
    }

    fun sendIntent(intent: SettingDataMigrationSendIntent) {
        when (intent) {
            SettingDataMigrationSendIntent.OnClickRenewCode -> startSession()
            SettingDataMigrationSendIntent.OnClickRecheck -> startIfIdle()
        }
    }

    private fun startIfIdle() {
        _uiState.update { it.copy(step = DataMigrationSendStep.Starting) }
        viewModelScope.launch {
            val runningWorks = runningWorkMonitor.runningWorks()
            if (runningWorks.isEmpty()) {
                startSession()
            } else {
                _uiState.update { it.copy(step = DataMigrationSendStep.Blocked(runningWorks)) }
            }
        }
    }

    private fun startSession() {
        dataMigrationSendController.start()
        hasStarted = true
        _uiState.update { it.copy(step = dataMigrationSendController.status.value.toStep()) }
    }

    private fun DataMigrationSendStatus.toStep(): DataMigrationSendStep = when (this) {
        DataMigrationSendStatus.Idle -> DataMigrationSendStep.Disconnected
        is DataMigrationSendStatus.WaitingForPair -> DataMigrationSendStep.WaitingForPair(code)
        is DataMigrationSendStatus.Paired -> DataMigrationSendStep.Sending(peerName)
        is DataMigrationSendStatus.Finished -> DataMigrationSendStep.Finished(newAudioMediaCount, failedCount)
        DataMigrationSendStatus.Locked -> DataMigrationSendStep.Locked
    }

    override fun onCleared() {
        if (hasStarted) dataMigrationSendController.stop()
    }
}
