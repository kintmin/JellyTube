package com.kintmin.presentation.ui.setting.backup_restore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupVersionUnsupportedException
import com.kintmin.domain.backup.model.RestorePreview
import com.kintmin.domain.backup.usecase.BackupNowUseCase
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.domain.backup.usecase.FindBackupToRestoreUseCase
import com.kintmin.domain.backup.usecase.PreviewRestoreUseCase
import com.kintmin.domain.backup.usecase.RegisterBackupFolderUseCase
import com.kintmin.domain.backup.usecase.RestoreBackupUseCase
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.device.usecase.FetchConnectionStatusUseCase
import com.kintmin.presentation.port.BulkDownloadKeepAlive
import com.kintmin.presentation.port.RunningTaskMonitor
import com.kintmin.presentation.port.model.BulkDownloadTask
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus
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
import kotlin.time.Clock

class SettingBackupRestoreViewModel(
    fetchBackupStatusFlowUseCase: FetchBackupStatusFlowUseCase,
    private val backupNowUseCase: BackupNowUseCase,
    private val registerBackupFolderUseCase: RegisterBackupFolderUseCase,
    private val findBackupToRestoreUseCase: FindBackupToRestoreUseCase,
    private val previewRestoreUseCase: PreviewRestoreUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val fetchConnectionStatusUseCase: FetchConnectionStatusUseCase,
    private val runningTaskMonitor: RunningTaskMonitor,
    private val bulkDownloadKeepAlive: BulkDownloadKeepAlive,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingBackupRestoreUiState())
    val uiState: StateFlow<SettingBackupRestoreUiState> = _uiState.asStateFlow()

    private val _eventFlow = Channel<SettingBackupRestoreEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SettingBackupRestoreEvent> = _eventFlow.receiveAsFlow()

    /** 미리보기부터 복원이 끝날 때까지 들고 있다. 다시 시도는 같은 백업으로 한다. */
    private var pendingBackup: BackupContent? = null
    private var runJob: Job? = null

    init {
        viewModelScope.launch {
            fetchBackupStatusFlowUseCase().collect { status ->
                _uiState.update {
                    it.copy(
                        folderLabel = status.folder?.label,
                        backupDescription = status.toDescription(),
                        isBackupFailing = status.isFailing,
                    )
                }
            }
        }
        findBackup()
    }

    fun sendIntent(intent: SettingBackupRestoreIntent) {
        when (intent) {
            SettingBackupRestoreIntent.OnClickBackupNow -> backupNow()
            SettingBackupRestoreIntent.OnClickPickFolder -> send(SettingBackupRestoreEvent.LaunchBackupFolderPicker)
            is SettingBackupRestoreIntent.OnBackupFolderPicked -> registerFolder(BackupFolder(intent.folderId, intent.label))
            SettingBackupRestoreIntent.OnClickStop -> stop()
            SettingBackupRestoreIntent.OnClickStartRestore -> startRestore()
            SettingBackupRestoreIntent.OnConfirmCellularRestore -> run()
            SettingBackupRestoreIntent.OnDismissCellularRestore -> updatePreview { it.copy(askCellular = false) }
            SettingBackupRestoreIntent.OnClickCloseResult -> {
                pendingBackup = null
                findBackup()
            }
            SettingBackupRestoreIntent.OnClickRetry -> run()
            SettingBackupRestoreIntent.OnConfirmExit -> exit()
        }
    }

    private fun findBackup() {
        updateRestore(RestorePhase.Finding)
        viewModelScope.launch {
            val found = findBackupToRestoreUseCase()
            val backup = found.getOrNull()
            when {
                backup != null -> preview(backup)
                found.isFailure -> {
                    pendingBackup = null
                    updateRestore(RestorePhase.ReadFailed(found.exceptionOrNull().toReadMessage()))
                }
                else -> {
                    pendingBackup = null
                    updateRestore(RestorePhase.NotFound)
                }
            }
        }
    }

    private fun backupNow() {
        if (_uiState.value.isBackingUp) return
        _uiState.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            val result = backupNowUseCase(nowMillis())
            _uiState.update { it.copy(isBackingUp = false) }
            send(SettingBackupRestoreEvent.ShowToast(result.toBackupMessage()))
            if (isRestoreIdle()) findBackup()
        }
    }

    /** 복원이 진행 중이거나 결과를 보여 주는 동안은 복원 구역을 바꾸지 않는다. */
    private fun isRestoreIdle(): Boolean = when (_uiState.value.restore) {
        RestorePhase.Finding, RestorePhase.NotFound, is RestorePhase.ReadFailed, is RestorePhase.Preview -> true
        else -> false
    }

    private fun registerFolder(folder: BackupFolder) {
        _uiState.update { it.copy(isBackingUp = true) }
        viewModelScope.launch {
            val result = registerBackupFolderUseCase(folder, nowMillis())
            _uiState.update { it.copy(isBackingUp = false) }
            send(SettingBackupRestoreEvent.ShowToast(result.toBackupMessage()))
            findBackup()
        }
    }

    private suspend fun preview(backup: BackupContent) {
        previewRestoreUseCase(backup)
            .onSuccess {
                pendingBackup = backup
                updateRestore(RestorePhase.Preview(it.toItem()))
            }
            .onFailure { updateRestore(RestorePhase.ReadFailed(it.toReadMessage())) }
    }

    /** Wi-Fi 가 아니면 막지 않고 한 번 묻는다. 연결 상태를 모르면 묻지 않는다. */
    private fun startRestore() {
        val hasDownloads = ((_uiState.value.restore as? RestorePhase.Preview)?.preview?.redownloadCount ?: 0) > 0
        val isWifi = fetchConnectionStatusUseCase().getOrNull()?.isWifi ?: true
        if (isWifi || !hasDownloads) run() else updatePreview { it.copy(askCellular = true) }
    }

    private fun run() {
        val backup = pendingBackup ?: return
        _uiState.update {
            it.copy(
                restore = RestorePhase.Running,
                stages = SettingBackupRestoreUiState.RESTORE_STAGES.map { kind ->
                    DataMigrationStageItem(kind, DataMigrationStageStatus.PENDING)
                },
            )
        }
        runJob = viewModelScope.launch {
            // 복원 중에 다른 작업이 DB 를 바꾸면 계획이 어긋난다.
            val runningTasks = runningTaskMonitor.runningTasks()
            if (runningTasks.isNotEmpty()) {
                updateRestore(RestorePhase.Blocked(runningTasks))
                return@launch
            }
            bulkDownloadKeepAlive.keepAlive(BulkDownloadTask.BACKUP_RESTORE) {
                restoreBackupUseCase(backup) { progress -> updateStage(progress.toStageItem()) }
            }
                .onSuccess { finish(it) }
                .onFailure { interrupt(failureMessage()) }
        }
    }

    private fun stop() {
        if (!_uiState.value.canStop) return
        viewModelScope.launch {
            runJob?.cancelAndJoin()
            interrupt(cause = null)
        }
    }

    /** 원인을 하나로 짚을 수 있는 것은 연결뿐이다. 나머지는 앱 로그로 보낸다. */
    private fun failureMessage(): String =
        if (fetchConnectionStatusUseCase().getOrNull()?.isConnected == false) {
            "인터넷 연결이 끊겼습니다. 연결한 뒤 다시 시도해 주세요."
        } else {
            "곡을 받다가 문제가 생겼습니다. 계속되면 설정 › 앱 로그 보기에서 원인을 확인할 수 있습니다."
        }

    /** 다시 받기 단계는 실패한 곡도 끝난 곡으로 세며 진행했으므로, 끝나면 실제로 받은 곡 수로 바꾼다. */
    private fun finish(summary: MigrationSummary) {
        val redownloadFailedCount = summary.failures.count { it.reason != MigrationFailureReason.NOT_RESTORABLE }
        _uiState.update { state ->
            state.copy(
                restore = RestorePhase.Finished(
                    newAudioMediaCount = summary.newAudioMediaCount,
                    newPlaylistCount = summary.newPlaylistCount,
                    failures = summary.failures.map { DataMigrationFailureItem(it.audioMediaName, it.reason.toText()) },
                ),
                stages = state.stages.map {
                    if (it.kind == DataMigrationStageKind.REDOWNLOAD && redownloadFailedCount > 0) {
                        it.copy(status = DataMigrationStageStatus.FAILED, doneCount = it.totalCount - redownloadFailedCount)
                    } else {
                        it.copy(status = DataMigrationStageStatus.DONE)
                    }
                },
            )
        }
        // 복원으로 이 기기가 백업을 따라잡았다. 쉬던 자동 백업을 다시 시작하도록 바로 한 번 쓴다.
        viewModelScope.launch { backupNowUseCase(nowMillis()) }
    }

    private fun interrupt(cause: String?) {
        _uiState.update { state ->
            state.copy(
                restore = RestorePhase.Interrupted(
                    listOfNotNull(cause, "다시 시도하면 받아 둔 곡은 건너뛰고 빠진 곡만 받습니다.").joinToString(" "),
                ),
                stages = state.stages.map {
                    if (it.status == DataMigrationStageStatus.IN_PROGRESS) it.copy(status = DataMigrationStageStatus.FAILED) else it
                },
            )
        }
    }

    /** 받아 둔 곡은 지우지 않는다. 같은 백업으로 다시 복원하면 이어 받는다. */
    private fun exit() {
        viewModelScope.launch {
            runJob?.cancelAndJoin()
            _eventFlow.send(SettingBackupRestoreEvent.NavigateToBack)
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

    private fun updateRestore(phase: RestorePhase) {
        _uiState.update { it.copy(restore = phase) }
    }

    private fun updatePreview(transform: (RestorePhase.Preview) -> RestorePhase.Preview) {
        _uiState.update { state ->
            val phase = state.restore as? RestorePhase.Preview ?: return@update state
            state.copy(restore = transform(phase))
        }
    }

    private fun send(event: SettingBackupRestoreEvent) {
        viewModelScope.launch { _eventFlow.send(event) }
    }

    private fun nowMillis() = Clock.System.now().toEpochMilliseconds()

    private fun RestorePreview.toItem() = RestorePreviewItem(
        createdAt = createdAtMillis.toDateTimeText(),
        totalCount = totalCount,
        alreadyOnDeviceCount = alreadyOnDeviceCount,
        redownloadCount = redownloadCount,
        notRestorableCount = notRestorableCount,
        estimatedMegabytes = estimatedDownloadBytes / MEGABYTE,
    )

    private fun MigrationProgress.toStageItem() = DataMigrationStageItem(
        kind = when (stage) {
            MigrationStage.CLEANUP, MigrationStage.PLANNING -> DataMigrationStageKind.PLANNING
            MigrationStage.REDOWNLOAD, MigrationStage.TRANSFER -> DataMigrationStageKind.REDOWNLOAD
            MigrationStage.APPLY -> DataMigrationStageKind.APPLY
        },
        status = DataMigrationStageStatus.IN_PROGRESS,
        doneCount = doneCount,
        totalCount = totalCount,
    )

    private fun Result<BackupOutcome>.toBackupMessage(): String = fold(
        onSuccess = { outcome ->
            when (outcome) {
                BackupOutcome.WRITTEN -> "백업했습니다."
                BackupOutcome.SKIPPED_EMPTY -> "음원이 없어 백업하지 않았습니다. 남아 있던 백업은 그대로 둡니다."
            }
        },
        onFailure = {
            when (it) {
                is BackupFolderUnavailableException -> "백업 폴더에 쓸 수 없습니다."
                else -> "백업하지 못했습니다. 앱 로그에서 원인을 확인할 수 있습니다."
            }
        },
    )

    private fun Throwable?.toReadMessage(): String = when (this) {
        is BackupVersionUnsupportedException -> "이 앱보다 새 버전에서 만든 백업입니다. 앱을 업데이트한 뒤 다시 시도해 주세요."
        is BackupFileInvalidException -> "백업 파일이 손상됐습니다. 다른 폴더에 백업이 있으면 그 폴더를 골라 주세요."
        is BackupFolderUnavailableException -> "백업 폴더에 접근할 수 없습니다. 폴더를 다시 지정해 주세요."
        else -> "백업 파일을 읽지 못했습니다."
    }

    private fun MigrationFailureReason.toText(): String = when (this) {
        MigrationFailureReason.NOT_RESTORABLE -> "원본이 유튜브 링크가 아니라 다시 받을 수 없음"
        MigrationFailureReason.REDOWNLOAD_FAILED -> "원본에서 다시 받지 못함"
        MigrationFailureReason.UNSUPPORTED_FORMAT -> "이 기기에서 재생할 수 없는 형식"
        MigrationFailureReason.TRANSFER_FAILED -> "전송 실패"
    }

    private companion object {
        const val MEGABYTE = 1_000_000L
    }
}
