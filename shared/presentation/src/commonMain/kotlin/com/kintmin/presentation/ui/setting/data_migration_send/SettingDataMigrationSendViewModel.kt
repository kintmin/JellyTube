package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.model.DataMigrationSendStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이 화면이 떠 있는 동안만 이전 엔드포인트가 열린다. 데이터 정리는 받는 기기가 연결한 뒤 서버가 한다. */
class SettingDataMigrationSendViewModel constructor(
    private val dataMigrationSendController: DataMigrationSendController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationSendUiState())
    val uiState: StateFlow<SettingDataMigrationSendUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    init {
        startSession()
        viewModelScope.launch {
            dataMigrationSendController.status.collect { status ->
                if (hasStarted) status.toStep()?.let { step -> _uiState.update { it.copy(step = step) } }
            }
        }
    }

    fun sendIntent(intent: SettingDataMigrationSendIntent) {
        when (intent) {
            SettingDataMigrationSendIntent.OnClickRenewCode -> startSession()
        }
    }

    private fun startSession() {
        dataMigrationSendController.start()
        hasStarted = true
        dataMigrationSendController.status.value.toStep()?.let { step -> _uiState.update { it.copy(step = step) } }
    }

    private fun DataMigrationSendStatus.toStep(): DataMigrationSendStep? = when (this) {
        DataMigrationSendStatus.Idle -> null
        is DataMigrationSendStatus.WaitingForPair -> DataMigrationSendStep.WaitingForPair(code)
        DataMigrationSendStatus.Paired -> DataMigrationSendStep.Sending
        is DataMigrationSendStatus.Finished -> DataMigrationSendStep.Finished(newAudioMediaCount, failedCount)
        DataMigrationSendStatus.Locked -> DataMigrationSendStep.Locked
    }

    override fun onCleared() {
        if (hasStarted) dataMigrationSendController.stop()
    }
}
