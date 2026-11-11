package com.kintmin.presentation.ui.setting.data_migration_receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationVersionMismatchException
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.PairMigrationSourceUseCase
import com.kintmin.presentation.platform.keepAppAwakeWhile
import com.kintmin.presentation.port.MigrationSourceDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingDataMigrationReceiveViewModel constructor(
    private val migrationSourceDiscovery: MigrationSourceDiscovery,
    private val pairMigrationSourceUseCase: PairMigrationSourceUseCase,
    private val importMigrationDataUseCase: ImportMigrationDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationReceiveUiState())
    val uiState: StateFlow<SettingDataMigrationReceiveUiState> = _uiState.asStateFlow()

    private var endpoint: MigrationEndpoint? = null

    /** 페어링에 성공한 뒤에는 실패해도 코드를 다시 받지 않고 이어 받는다. */
    private var token: String? = null

    fun sendIntent(intent: SettingDataMigrationReceiveIntent) {
        when (intent) {
            SettingDataMigrationReceiveIntent.OnClickSearch -> search()
            is SettingDataMigrationReceiveIntent.OnChangeCode -> _uiState.update { state ->
                state.copy(code = intent.code.filter(Char::isDigit).take(SettingDataMigrationReceiveUiState.CODE_LENGTH))
            }
            SettingDataMigrationReceiveIntent.OnClickConnect -> connect()
            SettingDataMigrationReceiveIntent.OnClickRetry -> if (token != null) import() else search()
        }
    }

    private fun search() {
        token = null
        updateStep(DataMigrationReceiveStep.Searching)
        viewModelScope.launch {
            endpoint = migrationSourceDiscovery.discover()
            updateStep(if (endpoint != null) DataMigrationReceiveStep.CodeInput() else DataMigrationReceiveStep.NotFound)
        }
    }

    private fun connect() {
        val endpoint = endpoint ?: return search()
        if (!_uiState.value.canConnect) return
        updateStep(DataMigrationReceiveStep.Pairing)
        viewModelScope.launch {
            pairMigrationSourceUseCase(endpoint, _uiState.value.code)
                .onSuccess { issued ->
                    token = issued
                    import()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(code = "", step = DataMigrationReceiveStep.CodeInput(error.toMessage())) }
                }
        }
    }

    private fun import() {
        val endpoint = endpoint ?: return search()
        val token = token ?: return search()
        updateStep(DataMigrationReceiveStep.Transferring(receivedBytes = 0, totalBytes = 0))
        viewModelScope.launch {
            keepAppAwakeWhile {
                importMigrationDataUseCase(endpoint, token) { progress ->
                    updateStep(DataMigrationReceiveStep.Transferring(progress.receivedBytes, progress.totalBytes))
                }
            }
                .onSuccess { summary ->
                    updateStep(DataMigrationReceiveStep.Completed(summary.newAudioMediaCount, summary.newPlaylistCount))
                }
                .onFailure { error -> updateStep(DataMigrationReceiveStep.Failed(error.toMessage())) }
        }
    }

    private fun updateStep(step: DataMigrationReceiveStep) {
        _uiState.update { it.copy(step = step) }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is MigrationPairRejectedException -> "코드가 맞지 않거나 Android 기기의 보내기 화면이 닫혔습니다."
        is MigrationVersionMismatchException -> message.orEmpty()
        else -> "연결이 끊겼습니다. 같은 Wi-Fi인지 확인하고 다시 시도해 주세요. 받은 파일은 이어서 받습니다."
    }
}
