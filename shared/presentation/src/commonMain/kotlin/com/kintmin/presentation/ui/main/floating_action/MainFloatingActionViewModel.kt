package com.kintmin.presentation.ui.main.floating_action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.youtube.YoutubeUrl
import com.kintmin.domain.youtube.YoutubeUrlKind
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
    private val pendingDownloadChoice = MutableStateFlow<PendingDownloadChoice?>(null)

    private val eventChannel = Channel<MainFloatingActionEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    val uiState: StateFlow<MainFloatingActionUiState> = combine(
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isPlaylistBottomSheetVisible,
        isCreatePlaylistDialogVisible,
        pendingDownloadChoice,
    ) { downloadTarget, isBottomSheetVisible, isCreateDialogVisible, pendingChoice ->
        val selectablePlaylistList = downloadTarget.selectable.map { playlist ->
            DownloadPlaylistUiState(
                id = playlist.id,
                name = if (playlist.type == PlaylistType.UNCATEGORIZED) PlaylistType.UNCATEGORIZED.defaultName else playlist.name,
                isSelected = playlist.id == downloadTarget.targetId,
            )
        }

        MainFloatingActionUiState(
            playlistIdOnDownload = downloadTarget.targetId,
            playlistIdOnDownloadName = selectablePlaylistList.firstOrNull { it.isSelected }?.name
                ?: if (downloadTarget.targetId == null) "불러오는 중" else PlaylistType.UNCATEGORIZED.defaultName,
            isDownloadTargetReady = downloadTarget.targetId != null,
            selectablePlaylistList = selectablePlaylistList,
            isPlaylistBottomSheetVisible = isBottomSheetVisible,
            isCreatePlaylistDialogVisible = isCreateDialogVisible,
            downloadChoiceUrl = pendingChoice?.url,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MainFloatingActionUiState(),
    )

    fun sendIntent(intent: MainFloatingActionIntent) {
        when (intent) {
            is MainFloatingActionIntent.OnClickDownload -> {
                handleClickDownload(intent.url, intent.playlistId)
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
            // M3 시트는 루트의 imePadding 때문에 위에 띄운 다이얼로그의 키보드를 따라 올라간다. 겹쳐 띄우지 않는다.
            MainFloatingActionIntent.OnClickCreatePlaylist -> {
                isPlaylistBottomSheetVisible.value = false
                isCreatePlaylistDialogVisible.value = true
            }
            MainFloatingActionIntent.OnDismissCreatePlaylistDialog -> {
                if (isCreatePlaylistDialogVisible.value) {
                    isCreatePlaylistDialogVisible.value = false
                    isPlaylistBottomSheetVisible.value = true
                }
            }
            is MainFloatingActionIntent.OnCreatePlaylist -> {
                isCreatePlaylistDialogVisible.value = false
                createPlaylistAsDownloadTarget(intent.title)
            }
            MainFloatingActionIntent.OnChooseSingleDownload -> {
                pendingDownloadChoice.value?.let { send(MainFloatingActionEvent.Download(it.url, it.playlistId)) }
                pendingDownloadChoice.value = null
            }
            MainFloatingActionIntent.OnChoosePlaylistDownload -> {
                pendingDownloadChoice.value?.let { send(MainFloatingActionEvent.NavigateToPlaylistDownload(it.url)) }
                pendingDownloadChoice.value = null
            }
            MainFloatingActionIntent.OnDismissDownloadChoice -> {
                pendingDownloadChoice.value = null
            }
        }
    }

    private fun updatePlaylistIdOnDownload(playlistId: Int) {
        viewModelScope.launch {
            updatePlaylistIdOnDownloadUseCase(playlistId)
            isPlaylistBottomSheetVisible.value = false
        }
    }

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

    private fun handleClickDownload(url: String, playlistId: Int) {
        when (YoutubeUrl.kindOf(url)) {
            YoutubeUrlKind.VIDEO -> send(MainFloatingActionEvent.Download(url, playlistId))
            YoutubeUrlKind.PLAYLIST -> send(MainFloatingActionEvent.NavigateToPlaylistDownload(url))
            YoutubeUrlKind.VIDEO_IN_PLAYLIST -> pendingDownloadChoice.value = PendingDownloadChoice(url, playlistId)
            YoutubeUrlKind.NOT_DOWNLOADABLE -> send(MainFloatingActionEvent.ShowToast("다운받을 영상이나 재생목록을 골라주세요."))
        }
    }

    private fun send(event: MainFloatingActionEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private data class PendingDownloadChoice(val url: String, val playlistId: Int)
}

