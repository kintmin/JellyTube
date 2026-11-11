package com.kintmin.presentation.ui.setting.data_migration_receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationPairRejectedException
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSessionExpiredException
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.model.MigrationVersionMismatchException
import com.kintmin.domain.data_migration.usecase.ClearMigrationStagingUseCase
import com.kintmin.domain.data_migration.usecase.ImportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.PairMigrationSourceUseCase
import com.kintmin.presentation.platform.continueInBackgroundWhile
import com.kintmin.presentation.port.MigrationSourceDiscovery
import com.kintmin.presentation.port.RunningWorkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingDataMigrationReceiveViewModel constructor(
    private val migrationSourceDiscovery: MigrationSourceDiscovery,
    private val pairMigrationSourceUseCase: PairMigrationSourceUseCase,
    private val importMigrationDataUseCase: ImportMigrationDataUseCase,
    private val cleanupAnomalousDataUseCase: CleanupAnomalousDataUseCase,
    private val clearMigrationStagingUseCase: ClearMigrationStagingUseCase,
    private val runningWorkMonitor: RunningWorkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingDataMigrationReceiveUiState())
    val uiState: StateFlow<SettingDataMigrationReceiveUiState> = _uiState.asStateFlow()

    private val _eventFlow = Channel<SettingDataMigrationReceiveEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SettingDataMigrationReceiveEvent> = _eventFlow.receiveAsFlow()

    private var endpoint: MigrationEndpoint? = null

    /** 페어링에 성공한 뒤에는 실패해도 코드를 다시 받지 않고 이어 받는다. */
    private var token: String? = null

    private var runJob: Job? = null

    fun sendIntent(intent: SettingDataMigrationReceiveIntent) {
        when (intent) {
            SettingDataMigrationReceiveIntent.OnClickSearch -> search()
            is SettingDataMigrationReceiveIntent.OnChangeCode -> _uiState.update { state ->
                state.copy(code = intent.code.filter(Char::isDigit).take(SettingDataMigrationReceiveUiState.CODE_LENGTH))
            }
            SettingDataMigrationReceiveIntent.OnClickConnect -> connect()
            SettingDataMigrationReceiveIntent.OnClickRetry -> if (token != null) run() else search()
            SettingDataMigrationReceiveIntent.OnConfirmExit -> exit()
        }
    }

    private fun search() {
        token = null
        updatePhase(DataMigrationReceivePhase.Searching)
        viewModelScope.launch {
            val runningWorks = runningWorkMonitor.runningWorks()
            if (runningWorks.isNotEmpty()) {
                updatePhase(DataMigrationReceivePhase.Blocked(runningWorks))
                return@launch
            }
            endpoint = migrationSourceDiscovery.discover()
            updatePhase(if (endpoint != null) DataMigrationReceivePhase.CodeInput() else DataMigrationReceivePhase.NotFound)
        }
    }

    private fun connect() {
        val endpoint = endpoint ?: return search()
        if (!_uiState.value.canConnect) return
        updatePhase(DataMigrationReceivePhase.Pairing)
        viewModelScope.launch {
            pairMigrationSourceUseCase(endpoint, _uiState.value.code)
                .onSuccess { issued ->
                    token = issued
                    run()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(code = "", phase = DataMigrationReceivePhase.CodeInput(error.toMessage())) }
                }
        }
    }

    private fun run() {
        val endpoint = endpoint ?: return search()
        val token = token ?: return search()
        _uiState.update {
            it.copy(
                phase = DataMigrationReceivePhase.Running,
                stages = DataMigrationStageKind.entries.map { kind -> DataMigrationStageItem(kind, DataMigrationStageStatus.PENDING) },
            )
        }
        runJob = viewModelScope.launch {
            // 지난 시도가 강제 종료로 남긴 파일을 먼저 치운다. 실패해도 이전은 할 수 있다.
            updateStage(DataMigrationStageItem(DataMigrationStageKind.CLEANUP, DataMigrationStageStatus.IN_PROGRESS))
            cleanupAnomalousDataUseCase()
            updateStage(DataMigrationStageItem(DataMigrationStageKind.CLEANUP, DataMigrationStageStatus.DONE))

            continueInBackgroundWhile {
                importMigrationDataUseCase(endpoint, token) { progress -> updateStage(progress.toStageItem()) }
            }
                .onSuccess { summary -> finish(summary) }
                .onFailure { error -> interrupt(error) }
        }
    }

    private fun finish(summary: MigrationSummary) {
        _uiState.update { state ->
            state.copy(
                phase = DataMigrationReceivePhase.Finished(
                    newAudioMediaCount = summary.newAudioMediaCount,
                    newPlaylistCount = summary.newPlaylistCount,
                    redownloadedCount = summary.redownloadedCount,
                    failures = summary.failures.map { DataMigrationFailureItem(it.audioMediaName, it.reason.toText()) },
                ),
                stages = state.stages.map { it.copy(status = DataMigrationStageStatus.DONE) },
            )
        }
    }

    private fun interrupt(error: Throwable) {
        if (error is MigrationSessionExpiredException) {
            token = null
            _uiState.update {
                it.copy(code = "", stages = emptyList(), phase = DataMigrationReceivePhase.CodeInput(error.toMessage()))
            }
            return
        }
        _uiState.update { state ->
            state.copy(
                phase = DataMigrationReceivePhase.Interrupted(error.toMessage()),
                stages = state.stages.map {
                    if (it.status == DataMigrationStageStatus.IN_PROGRESS) it.copy(status = DataMigrationStageStatus.FAILED) else it
                },
            )
        }
    }

    private fun exit() {
        val phase = _uiState.value.phase
        viewModelScope.launch {
            // 받던 파일을 지우기 전에 받기를 먼저 멈춘다. 반영 중이었다면 트랜잭션째 되돌아간다.
            runJob?.cancelAndJoin()
            // 다시 시도하지 않고 나가면 받아 둔 임시 파일은 쓸 데가 없다.
            if (phase == DataMigrationReceivePhase.Running || phase is DataMigrationReceivePhase.Interrupted) {
                clearMigrationStagingUseCase()
            }
            _eventFlow.send(SettingDataMigrationReceiveEvent.NavigateToBack)
        }
    }

    /** 이번 단계보다 앞선 단계는 끝난 것이다. */
    private fun updateStage(item: DataMigrationStageItem) {
        _uiState.update { state ->
            state.copy(
                stages = state.stages.map {
                    when {
                        it.kind == item.kind -> item
                        it.kind.ordinal < item.kind.ordinal -> it.copy(status = DataMigrationStageStatus.DONE)
                        else -> it
                    }
                },
            )
        }
    }

    private fun updatePhase(phase: DataMigrationReceivePhase) {
        _uiState.update { it.copy(phase = phase) }
    }

    private fun MigrationProgress.toStageItem() = DataMigrationStageItem(
        kind = when (stage) {
            MigrationStage.PLANNING -> DataMigrationStageKind.PLANNING
            MigrationStage.REDOWNLOAD -> DataMigrationStageKind.REDOWNLOAD
            MigrationStage.TRANSFER -> DataMigrationStageKind.TRANSFER
            MigrationStage.APPLY -> DataMigrationStageKind.APPLY
        },
        status = DataMigrationStageStatus.IN_PROGRESS,
        doneCount = doneCount,
        totalCount = totalCount,
        receivedBytes = receivedBytes,
        totalBytes = totalBytes,
    )

    private fun MigrationFailureReason.toText(): String = when (this) {
        MigrationFailureReason.UNSUPPORTED_FORMAT -> "iPhone에서 재생할 수 없는 형식"
        MigrationFailureReason.REDOWNLOAD_FAILED -> "원본에서 다시 받을 수 없음"
        MigrationFailureReason.TRANSFER_FAILED -> "전송 실패"
    }

    private fun Throwable.toMessage(): String = when (this) {
        is MigrationPairRejectedException -> "코드가 맞지 않거나 Android 기기의 보내기 화면이 닫혔습니다."
        is MigrationVersionMismatchException -> message.orEmpty()
        is MigrationSessionExpiredException -> "Android 기기와의 연결이 끊겼습니다. Android 화면에 보이는 코드를 다시 입력해 주세요. 받은 파일은 이어서 받습니다."
        else -> "연결이 끊겼습니다. 두 기기가 같은 Wi-Fi에 있고 Android 보내기 화면이 열려 있는지 확인한 뒤 다시 시도해 주세요. 받은 파일은 이어서 받습니다."
    }
}
