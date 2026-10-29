package com.kintmin.presentation.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdateIsStepEnabledUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.app_setting.usecase.UpdateShouldInsertAtTopOnDownloadUseCase
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportSharedAudioMediaUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.platform.worker.usecase.ExecuteAnomalyDataCheck
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingViewModel constructor(
    fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase,
    fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase,
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    fetchIsStepEnabledFlowUseCase: FetchIsStepEnabledFlowUseCase,
    private val updateShouldInsertAtTopOnDownloadUseCase: UpdateShouldInsertAtTopOnDownloadUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
    private val updateIsStepEnabledUseCase: UpdateIsStepEnabledUseCase,
    private val executeAnomalyDataCheck: ExecuteAnomalyDataCheck,
    private val importSharedAudioMediaUseCase: ImportSharedAudioMediaUseCase,
) : ViewModel() {

    private val isPlaylistIdOnDownloadBottomSheetVisible = MutableStateFlow(false)
    private val isImportPlaylistBottomSheetVisible = MutableStateFlow(false)
    private val isImporting = MutableStateFlow(false)
    private val _eventFlow = MutableSharedFlow<SettingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val isStepEnabledFlow = fetchIsStepEnabledFlowUseCase()

    /** 파일 선택기에서 고른 음원. 재생목록 BottomSheet에서 대상이 정해질 때까지 들고 있는다. */
    private var pendingImportUriStringList: List<String> = emptyList()

    val uiState: StateFlow<SettingUiState> = combine(
        fetchShouldInsertAtTopOnDownloadFlowUseCase(),
        fetchPlaylistIdOnDownloadFlowUseCase(),
        fetchAllPlaylistFlowUseCase(),
        isPlaylistIdOnDownloadBottomSheetVisible,
    ) { shouldInsertAtTopOnDownload, playlistIdOnDownload, playlistList, isBottomSheetVisible ->
        // 다운로드 대상은 USER 플레이리스트 + 미분류(기본)만. 전체/즐겨찾기는 제외.
        val uncategorizedId = playlistList.firstOrNull { it.type == PlaylistType.UNCATEGORIZED }?.id
        val effectiveTargetId =
            playlistIdOnDownload?.takeIf { id -> playlistList.any { it.id == id } } ?: uncategorizedId

        val selectablePlaylistList = playlistList
            .filter { it.type == PlaylistType.USER || it.type == PlaylistType.UNCATEGORIZED }
            .map { playlist ->
                DownloadPlaylistUiState(
                    id = playlist.id,
                    name = if (playlist.type == PlaylistType.UNCATEGORIZED) "기본" else playlist.name,
                    isSelected = playlist.id == effectiveTargetId,
                )
            }

        SettingUiState(
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
            playlistIdOnDownload = effectiveTargetId,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.isSelected }?.name ?: "기본",
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistIdOnDownloadBottomSheetVisible = isBottomSheetVisible,
        )
    }.combine(isStepEnabledFlow) { state, isStepEnabled ->
        state.copy(isStepEnabled = isStepEnabled)
    }.combine(isImportPlaylistBottomSheetVisible) { state, isVisible ->
        state.copy(isImportPlaylistBottomSheetVisible = isVisible)
    }.combine(isImporting) { state, isImportingNow ->
        state.copy(isImporting = isImportingNow)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingUiState(),
    )

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
                    _eventFlow.emit(SettingEvent.NavigateToStepScreen)
                }
            }
            SettingIntent.OnClickAnomalyDataCheckTile -> {
                viewModelScope.launch {
                    val started = executeAnomalyDataCheck()
                    _eventFlow.emit(
                        SettingEvent.ShowToast(
                            if (started) "이상 데이터 점검을 시작합니다.\n완료 시 알림이 도착합니다."
                            else "이상 데이터 점검중입니다.",
                        )
                    )
                }
            }
            SettingIntent.OnClickAppLogTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToAppLogScreen)
                }
            }
            SettingIntent.OnClickImportAudioTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.LaunchAudioFilePicker)
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
                // 대상 재생목록을 고르지 않았으면 가져오기를 취소한다.
                pendingImportUriStringList = emptyList()
                isImportPlaylistBottomSheetVisible.value = false
            }
            SettingIntent.OnClickDeviceSaveTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToDeviceSaveScreen)
                }
            }
            SettingIntent.OnClickShareTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToShareScreen)
                }
            }
            SettingIntent.OnClickFileShareReceiveTile -> {
                viewModelScope.launch {
                    _eventFlow.emit(SettingEvent.NavigateToFileShareReceiveScreen)
                }
            }
            is SettingIntent.OnToggleIsStepEnabled -> {
                if (intent.value) {
                    viewModelScope.launch {
                        _eventFlow.emit(SettingEvent.RequestActivityRecognitionPermission)
                    }
                } else {
                    viewModelScope.launch {
                        updateIsStepEnabledUseCase(false)
                        _eventFlow.emit(SettingEvent.StopStepForegroundService)
                    }
                }
            }
            SettingIntent.OnActivityRecognitionGranted -> {
                updateIsStepEnabled(true)
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
                _eventFlow.emit(SettingEvent.ShowToast("${successCount}개 파일을 가져왔습니다."))
            }
            errorList.distinct().forEach { error ->
                _eventFlow.emit(SettingEvent.ShowToast(error))
            }
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

