package com.kintmin.presentation.ui.setting.data_migration_send

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.presentation.port.DataMigrationSendController
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.secret_mode.SecretModeRunner
import com.kintmin.domain.data_migration.model.DataMigrationSendStatus
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.SettingDataMigrationSendScreenRoute
import com.kintmin.presentation.ui.setting.data_migration_send.navigation.settingDataMigrationSendTypeMap
import com.kintmin.presentation.ui.setting.data_migration_share.MigrationShareKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 이 화면이 떠 있는 동안만 이전 엔드포인트가 열린다. */
class SettingDataMigrationSendViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val dataMigrationSendController: DataMigrationSendController,
    private val runningWorkMonitor: RunningTaskMonitor,
    private val secretModeRunner: SecretModeRunner,
    private val fetchSecretModeEnabledFlowUseCase: FetchSecretModeEnabledFlowUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SettingDataMigrationSendScreenRoute>(
        settingDataMigrationSendTypeMap,
    )

    private val _uiState = MutableStateFlow(
        SettingDataMigrationSendUiState(kind = route.kind, sendingCount = route.idList.size),
    )
    val uiState: StateFlow<SettingDataMigrationSendUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    init {
        startIfIdle()
        viewModelScope.launch {
            dataMigrationSendController.status.collect { status ->
                if (hasStarted) _uiState.update { it.copy(step = status.toStep()) }
                // 보내는 중에 비밀모드가 꺼지면 이 화면까지 닫혀 받는 기기의 전송이 실패한다.
                if (status is DataMigrationSendStatus.Paired) secretModeRunner.pause() else secretModeRunner.resume()
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
            val runningTasks = runningWorkMonitor.runningTasks()
            if (runningTasks.isEmpty()) {
                startSession()
            } else {
                _uiState.update { it.copy(step = DataMigrationSendStep.Blocked(runningTasks)) }
            }
        }
    }

    private fun startSession() {
        dataMigrationSendController.start(scope())
        hasStarted = true
        _uiState.update { it.copy(step = dataMigrationSendController.status.value.toStep()) }
    }

    private fun scope(): MigrationScope = when (route.kind) {
        // 비밀 플레이리스트는 비밀모드가 켜져 있을 때만 함께 간다.
        MigrationShareKind.SYNC ->
            MigrationScope.Everything(includesSecret = fetchSecretModeEnabledFlowUseCase().value)
        MigrationShareKind.PLAYLIST -> MigrationScope.Playlists(route.idList.toSet())
        MigrationShareKind.AUDIO_MEDIA -> MigrationScope.AudioMedias(route.idList.toSet())
    }

    private fun DataMigrationSendStatus.toStep(): DataMigrationSendStep = when (this) {
        DataMigrationSendStatus.Idle -> DataMigrationSendStep.Disconnected
        is DataMigrationSendStatus.WaitingForPair -> DataMigrationSendStep.WaitingForPair(code)
        is DataMigrationSendStatus.Paired -> DataMigrationSendStep.Sending(peerName)
        is DataMigrationSendStatus.Finished -> DataMigrationSendStep.Finished(newAudioMediaCount, failedCount)
        DataMigrationSendStatus.Locked -> DataMigrationSendStep.Locked
    }

    override fun onCleared() {
        // 화면이 사라지는 길이 어디든 — 끝났든 중단했든 — 멈춰 둔 시계를 되돌린다.
        secretModeRunner.resume()
        if (hasStarted) dataMigrationSendController.stop()
    }
}
