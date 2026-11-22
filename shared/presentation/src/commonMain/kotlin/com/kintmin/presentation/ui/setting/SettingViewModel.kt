package com.kintmin.presentation.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdateIsStepEnabledUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.app_setting.usecase.UpdateShouldInsertAtTopOnDownloadUseCase
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.usecase.FetchBackupStatusFlowUseCase
import com.kintmin.domain.backup.usecase.HasLocalBackupUseCase
import com.kintmin.domain.backup.usecase.RegisterBackupFolderUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.user.usecase.FetchUserIdUseCase
import com.kintmin.presentation.port.StartAnomalyDataCheck
import com.kintmin.presentation.port.StepMeasurementController
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import com.kintmin.presentation.ui.setting.backup_restore.toDescription
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class SettingViewModel constructor(
    fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    fetchDownloadTargetPlaylistsFlowUseCase: FetchDownloadTargetPlaylistsFlowUseCase,
    fetchIsStepEnabledFlowUseCase: FetchIsStepEnabledFlowUseCase,
    fetchUserIdUseCase: FetchUserIdUseCase,
    private val fetchBackupStatusFlowUseCase: FetchBackupStatusFlowUseCase,
    private val registerBackupFolderUseCase: RegisterBackupFolderUseCase,
    private val hasLocalBackupUseCase: HasLocalBackupUseCase,
    private val updateShouldInsertAtTopOnDownloadUseCase: UpdateShouldInsertAtTopOnDownloadUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
    private val updateIsStepEnabledUseCase: UpdateIsStepEnabledUseCase,
    private val executeAnomalyDataCheck: StartAnomalyDataCheck,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
    private val stepMeasurementController: StepMeasurementController,
) : ViewModel() {

    private val isPlaylistIdOnDownloadBottomSheetVisible = MutableStateFlow(false)
    private val isImportPlaylistBottomSheetVisible = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val deviceId = MutableStateFlow<String?>(null)
    private val eventChannel = Channel<SettingEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SettingEvent> = eventChannel.receiveAsFlow()

    private val isStepEnabledFlow = fetchIsStepEnabledFlowUseCase()

    private var pendingImportUriStringList: List<String> = emptyList()

    val uiState: StateFlow<SettingUiState> = combine(
        fetchShouldInsertAtTopOnDownloadFlowUseCase(),
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isPlaylistIdOnDownloadBottomSheetVisible,
    ) { shouldInsertAtTopOnDownload, downloadTarget, isBottomSheetVisible ->
        val selectablePlaylistList = downloadTarget.selectable.map { playlist ->
            DownloadPlaylistUiState(
                id = playlist.id,
                name = if (playlist.type == PlaylistType.UNCATEGORIZED) PlaylistType.UNCATEGORIZED.defaultName else playlist.name,
                isSelected = playlist.id == downloadTarget.targetId,
            )
        }

        SettingUiState(
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
            playlistIdOnDownload = downloadTarget.targetId,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.isSelected }?.name ?: PlaylistType.UNCATEGORIZED.defaultName,
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistIdOnDownloadBottomSheetVisible = isBottomSheetVisible,
        )
    }.combine(isStepEnabledFlow) { state, isStepEnabled ->
        state.copy(isStepEnabled = isStepEnabled)
    }.combine(isImportPlaylistBottomSheetVisible) { state, isVisible ->
        state.copy(isImportPlaylistBottomSheetVisible = isVisible)
    }.combine(isImporting) { state, isImportingNow ->
        state.copy(isImporting = isImportingNow)
    }.combine(deviceId) { state, id ->
        state.copy(deviceId = id)
    }.combine(fetchBackupStatusFlowUseCase()) { state, backupStatus ->
        state.copy(backupDescription = backupStatus.toDescription())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingUiState(),
    )

    init {
        viewModelScope.launch { deviceId.value = fetchUserIdUseCase().getOrNull() }
    }

    fun sendIntent(intent: SettingIntent) {
        when (intent) {
            SettingIntent.OnInit -> Unit
            SettingIntent.OnClickShouldInsertAtTopOnDownloadTile -> {
                updateShouldInsertAtTopOnDownload(!uiState.value.shouldInsertAtTopOnDownload)
            }
            is SettingIntent.OnToggleShouldInsertAtTopOnDownload -> {
                updateShouldInsertAtTopOnDownload(intent.value)
            }
            SettingIntent.OnClickPlaylistIdOnDownloadTile -> {
                isPlaylistIdOnDownloadBottomSheetVisible.value = true
            }
            SettingIntent.OnDismissPlaylistIdOnDownloadBottomSheet -> {
                isPlaylistIdOnDownloadBottomSheetVisible.value = false
            }
            is SettingIntent.OnSelectPlaylistIdOnDownload -> {
                updatePlaylistIdOnDownload(intent.playlistId)
            }
            SettingIntent.OnClickStepTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToStepScreen)
                }
            }
            SettingIntent.OnClickBackupRestoreTile -> {
                viewModelScope.launch {
                    val hasFolder = fetchBackupStatusFlowUseCase().first().folder != null
                    eventChannel.send(
                        if (hasFolder || hasLocalBackupUseCase()) {
                            SettingEvent.NavigateToBackupRestoreScreen
                        } else {
                            SettingEvent.LaunchBackupFolderPicker
                        },
                    )
                }
            }
            is SettingIntent.OnBackupFolderPicked -> registerBackupFolder(BackupFolder(intent.folderId, intent.label))
            SettingIntent.OnClickAnomalyDataCheckTile -> {
                viewModelScope.launch {
                    val started = executeAnomalyDataCheck()
                    eventChannel.send(
                        SettingEvent.ShowToast(
                            if (started) "이상 데이터 점검을 시작합니다.\n완료 시 알림이 도착합니다."
                            else "이상 데이터 점검중입니다.",
                        )
                    )
                }
            }
            SettingIntent.OnClickAppLogTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToAppLogScreen)
                }
            }
            SettingIntent.OnClickImportAudioTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.LaunchAudioFilePicker)
                }
            }
            is SettingIntent.OnAudioFilesSelected -> {
                if (intent.uriStrings.isEmpty()) return
                pendingImportUriStringList = intent.uriStrings
                isImportPlaylistBottomSheetVisible.value = true
            }
            is SettingIntent.OnSelectImportPlaylist -> {
                importPendingAudioFiles(intent.playlistId)
            }
            SettingIntent.OnDismissImportPlaylistBottomSheet -> {
                pendingImportUriStringList = emptyList()
                isImportPlaylistBottomSheetVisible.value = false
            }
            SettingIntent.OnClickDeviceSaveTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToDeviceSaveScreen)
                }
            }
            SettingIntent.OnClickSecretPlaylistGuideTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToSecretPlaylistGuideScreen)
                }
            }
            SettingIntent.OnClickFileShareReceiveTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToFileShareReceiveScreen)
                }
            }
            SettingIntent.OnClickDataMigrationSendTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToDataMigrationSendScreen)
                }
            }
            SettingIntent.OnClickDataMigrationReceiveTile -> {
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.NavigateToDataMigrationReceiveScreen)
                }
            }
            SettingIntent.OnClickDeviceIdTile -> {
                val id = deviceId.value ?: return
                viewModelScope.launch {
                    eventChannel.send(SettingEvent.CopyDeviceId(id))
                }
            }
            is SettingIntent.OnToggleIsStepEnabled -> {
                if (intent.value) {
                    viewModelScope.launch {
                        eventChannel.send(SettingEvent.RequestActivityRecognitionPermission)
                    }
                } else {
                    viewModelScope.launch {
                        updateIsStepEnabledUseCase(false)
                        stepMeasurementController.stop()
                    }
                }
            }
            SettingIntent.OnActivityRecognitionGranted -> {
                updateIsStepEnabled(true)
                stepMeasurementController.start()
            }
            SettingIntent.OnActivityRecognitionDenied -> {
                updateIsStepEnabled(false)
            }
        }
    }

    private fun importPendingAudioFiles(playlistId: Int) {
        val uriStringList = pendingImportUriStringList
        pendingImportUriStringList = emptyList()
        isImportPlaylistBottomSheetVisible.value = false
        if (uriStringList.isEmpty()) return

        viewModelScope.launch {
            isImporting.value = true
            var successCount = 0
            val errorList = mutableListOf<String>()

            uriStringList.forEach { uriString ->
                importSharedAudioMediaUseCase(uriString, playlistIdOverride = playlistId)
                    .onSuccess { successCount++ }
                    .onFailure { throwable ->
                        errorList += when (throwable) {
                            is AlreadyDownloadedMedia -> "이미 저장된 파일입니다."
                            else -> throwable.message ?: "알 수 없는 오류"
                        }
                    }
            }
            isImporting.value = false

            if (successCount > 0) {
                eventChannel.send(SettingEvent.ShowToast("${successCount}개 파일을 가져왔습니다."))
            }
            errorList.distinct().forEach { error ->
                eventChannel.send(SettingEvent.ShowToast(error))
            }
        }
    }

    private fun registerBackupFolder(folder: BackupFolder) {
        viewModelScope.launch {
            val result = registerBackupFolderUseCase(folder, Clock.System.now().toEpochMilliseconds())
            val message = when {
                result.getOrNull() == BackupOutcome.WRITTEN -> "백업했습니다. 이제 하루 한 번 이 폴더에 백업합니다."
                result.getOrNull() == BackupOutcome.KEPT_EXISTING -> "이 폴더에서 백업을 찾았습니다. 복원할 수 있게 덮어쓰지 않았습니다."
                result.getOrNull() == BackupOutcome.KEPT_UNREADABLE -> "이 폴더의 백업을 읽지 못했습니다. 남은 백업일 수 있어 덮어쓰지 않았습니다. 앱 로그에서 원인을 확인할 수 있습니다."
                result.isSuccess -> "백업 폴더를 지정했습니다. 음원이 생기면 백업합니다."
                else -> "백업 폴더를 지정했지만 백업하지 못했습니다."
            }
            eventChannel.send(SettingEvent.ShowToast(message))
            eventChannel.send(SettingEvent.NavigateToBackupRestoreScreen)
        }
    }

    private fun updateShouldInsertAtTopOnDownload(value: Boolean) {
        viewModelScope.launch {
            updateShouldInsertAtTopOnDownloadUseCase(value)
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistIdOnDownloadBottomSheetVisible.value = false
        }
    }

    private fun updateIsStepEnabled(value: Boolean) {
        viewModelScope.launch {
            updateIsStepEnabledUseCase(value)
        }
    }
}

