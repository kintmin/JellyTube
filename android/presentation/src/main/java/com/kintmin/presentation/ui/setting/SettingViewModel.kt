package com.kintmin.presentation.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.FetchIsStepEnabledFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.UpdateIsStepEnabledUseCase
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.app_setting.usecase.UpdateShouldInsertAtTopOnDownloadUseCase
import com.kintmin.domain.playlist.model.Playlist
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
) : ViewModel() {

    private val isPlaylistIdOnDownloadBottomSheetVisible = MutableStateFlow(false)
    private val _eventFlow = MutableSharedFlow<SettingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val isStepEnabledFlow = fetchIsStepEnabledFlowUseCase()

    val uiState: StateFlow<SettingUiState> = combine(
        fetchShouldInsertAtTopOnDownloadFlowUseCase(),
        fetchPlaylistIdOnDownloadFlowUseCase(),
        fetchAllPlaylistFlowUseCase(),
        isPlaylistIdOnDownloadBottomSheetVisible,
    ) { shouldInsertAtTopOnDownload, playlistIdOnDownload, playlistList, isBottomSheetVisible ->
        val selectablePlaylistList = playlistList
            .filterNot { it.id == Playlist.TOTAL }
            .map { it.id to it.name }
            .toMutableList()
            .apply {
                if (none { it.first == Playlist.UNCATEGORIZED }) {
                    add(0, Playlist.UNCATEGORIZED to "미분류")
                }
            }
            .distinctBy { it.first }
            .map { (id, name) ->
                DownloadPlaylistUiState(
                    id = id,
                    name = toDisplayPlaylistName(id, name),
                    isSelected = id == playlistIdOnDownload,
                )
            }

        SettingUiState(
            shouldInsertAtTopOnDownload = shouldInsertAtTopOnDownload,
            playlistIdOnDownload = playlistIdOnDownload,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.id == playlistIdOnDownload }?.name ?: "기본",
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistIdOnDownloadBottomSheetVisible = isBottomSheetVisible,
        )
    }.combine(isStepEnabledFlow) { state, isStepEnabled ->
        state.copy(isStepEnabled = isStepEnabled)
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

    private fun toDisplayPlaylistName(playlistId: Int, playlistName: String): String {
        return if (playlistId == Playlist.UNCATEGORIZED) "기본" else playlistName
    }
}

