package com.kintmin.presentation.ui.main.floating_action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.youtube.YoutubeUrl
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainFloatingActionViewModel constructor(
    fetchDownloadTargetPlaylistsFlowUseCase: FetchDownloadTargetPlaylistsFlowUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
) : ViewModel() {

    private val isPlaylistBottomSheetVisible = MutableStateFlow(false)

    private val _eventFlow = MutableSharedFlow<MainFloatingActionEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val uiState: StateFlow<MainFloatingActionUiState> = combine(
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isPlaylistBottomSheetVisible,
    ) { downloadTarget, isBottomSheetVisible ->
        val selectablePlaylistList = downloadTarget.selectable.map { playlist ->
            DownloadPlaylistUiState(
                id = playlist.id,
                name = if (playlist.type == PlaylistType.UNCATEGORIZED) PlaylistType.UNCATEGORIZED.defaultName else playlist.name,
                isSelected = playlist.id == downloadTarget.targetId,
            )
        }

        MainFloatingActionUiState(
            playlistIdOnDownload = downloadTarget.targetId,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.isSelected }?.name ?: PlaylistType.UNCATEGORIZED.defaultName,
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistBottomSheetVisible = isBottomSheetVisible,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainFloatingActionUiState(),
    )

    fun sendIntent(intent: MainFloatingActionIntent) {
        when (intent) {
            is MainFloatingActionIntent.OnClickDownload -> {
                handleClickDownload(intent.url)
            }
            MainFloatingActionIntent.OnClickPlaylistButton -> {
                isPlaylistBottomSheetVisible.value = true
            }
            MainFloatingActionIntent.OnDismissPlaylistBottomSheet -> {
                isPlaylistBottomSheetVisible.value = false
            }
            is MainFloatingActionIntent.OnSelectPlaylist -> {
                updatePlaylistIdOnDownload(intent.playlistId)
            }
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistBottomSheetVisible.value = false
        }
    }

    private fun handleClickDownload(url: String) {
        viewModelScope.launch {
            if (YoutubeUrl.isDownloadable(url)) {
                _eventFlow.emit(MainFloatingActionEvent.Download(url))
            } else {
                _eventFlow.emit(MainFloatingActionEvent.ShowToast("다운받을 영상을 골라주세요."))
            }
        }
    }
}

