package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.presentation.port.DataMigrationSendController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 이 화면이 떠 있는 동안만 이전 엔드포인트가 열린다.
 *
 * 세션을 열기 전에 이상 데이터 정리를 끝낸다. 확장자가 내용과 다른 음원은 받는 iOS 에서 열리지 않고,
 * 고아 행·파일은 보낼 필요가 없다.
 */
class SettingDataMigrationSendViewModel constructor(
    private val dataMigrationSendController: DataMigrationSendController,
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationSendUiState())
    val uiState: StateFlow<SettingDataMigrationSendUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    init {
        prepareAndStart()
        viewModelScope.launch {
            dataMigrationSendController.pairingCode.collect { code ->
                _uiState.update { it.copy(pairingCode = code) }
            }
        }
    }

    fun sendIntent(intent: SettingDataMigrationSendIntent) {
        when (intent) {
            SettingDataMigrationSendIntent.OnClickRenewCode -> dataMigrationSendController.start()
            SettingDataMigrationSendIntent.OnClickRetryPreparation -> prepareAndStart()
        }
    }

    private fun prepareAndStart() {
        _uiState.update { it.copy(step = DataMigrationSendStep.Preparing) }
        viewModelScope.launch {
            cleanupAnomalousDataUseCase()
                .onSuccess {
                    dataMigrationSendController.start()
                    hasStarted = true
                    _uiState.update { it.copy(step = DataMigrationSendStep.Ready) }
                }
                .onFailure { _uiState.update { it.copy(step = DataMigrationSendStep.PreparationFailed) } }
        }
    }

    override fun onCleared() {
        if (hasStarted) dataMigrationSendController.stop()
    }
}
