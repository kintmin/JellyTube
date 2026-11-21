package com.kintmin.presentation.ui.main.floating_action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.youtube.YoutubeUrl
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainFloatingActionViewModel constructor(
    fetchDownloadTargetPlaylistsFlowUseCase: FetchDownloadTargetPlaylistsFlowUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
) : ViewModel() {

    private val isPlaylistBottomSheetVisible = MutableStateFlow(false)
    private val isCreatePlaylistDialogVisible = MutableStateFlow(false)

    private val eventChannel = Channel<MainFloatingActionEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    val uiState: StateFlow<MainFloatingActionUiState> = combine(
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isPlaylistBottomSheetVisible,
        isCreatePlaylistDialogVisible,
    ) { downloadTarget, isBottomSheetVisible, isCreateDialogVisible ->
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
            isCreatePlaylistDialogVisible = isCreateDialogVisible,
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
            MainFloatingActionIntent.OnClickCreatePlaylist -> {
                isCreatePlaylistDialogVisible.value = true
            }
            MainFloatingActionIntent.OnDismissCreatePlaylistDialog -> {
                isCreatePlaylistDialogVisible.value = false
            }
            is MainFloatingActionIntent.OnCreatePlaylist -> {
                createPlaylistAsDownloadTarget(intent.title)
            }
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistBottomSheetVisible.value = false
        }
    }

    /** 받을 재생목록을 고르다 만든 것이므로 곧바로 받을 곳으로 삼는다. */
    private fun createPlaylistAsDownloadTarget(title: String) {
        viewModelScope.launch {
            addNewPlaylistUseCase(title)
                .onSuccess { playlistId -> updatePlaylistIdOnDownload(playlistId) }
                .onFailure {
                    eventChannel.send(
                        MainFloatingActionEvent.ShowToast("플레이리스트를 만들지 못했습니다. 저장 공간을 확인하고 다시 시도해 주세요."),
                    )
                }
        }
    }

    private fun handleClickDownload(url: String) {
        viewModelScope.launch {
            if (YoutubeUrl.isDownloadable(url)) {
                eventChannel.send(MainFloatingActionEvent.Download(url))
            } else {
                eventChannel.send(MainFloatingActionEvent.ShowToast("다운받을 영상을 골라주세요."))
            }
        }
    }
}

