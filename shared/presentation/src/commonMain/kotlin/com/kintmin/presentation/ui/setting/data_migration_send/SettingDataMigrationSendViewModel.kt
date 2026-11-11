package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.presentation.port.DataMigrationSendController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이 화면이 떠 있는 동안만 이전 엔드포인트가 열린다. */
class SettingDataMigrationSendViewModel constructor(
    private val dataMigrationSendController: DataMigrationSendController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationSendUiState())
    val uiState: StateFlow<SettingDataMigrationSendUiState> = _uiState.asStateFlow()

    init {
        dataMigrationSendController.start()
        viewModelScope.launch {
            dataMigrationSendController.pairingCode.collect { code ->
                _uiState.update { it.copy(pairingCode = code) }
            }
        }
    }

    fun sendIntent(intent: SettingDataMigrationSendIntent) {
        when (intent) {
            SettingDataMigrationSendIntent.OnClickRenewCode -> dataMigrationSendController.start()
        }
    }

    override fun onCleared() {
        dataMigrationSendController.stop()
    }
}
