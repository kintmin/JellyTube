package com.kintmin.presentation.ui.main.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.app_setting.usecase.UpdatePlaylistIdOnDownloadUseCase
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.DeletePlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchDownloadTargetPlaylistsFlowUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistSequenceUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel constructor(
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    fetchDownloadTargetPlaylistsFlowUseCase: FetchDownloadTargetPlaylistsFlowUseCase,
    private val updatePlaylistIdOnDownloadUseCase: UpdatePlaylistIdOnDownloadUseCase,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val updatePlaylistSequenceUseCase: UpdatePlaylistSequenceUseCase,
) : ViewModel() {

    private val eventChannel = Channel<PlaylistEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    private val isReorderBottomSheetVisible = MutableStateFlow(false)

    // 비어 있다고 숨기지 않는다. 숨기면 즐겨찾기로 가는 입구가 화면에서 사라지고,
    // 2열 격자에서 항목이 생겼다 없어질 때 뒤의 카드 위치가 통째로 밀린다.
    val uiState: StateFlow<PlaylistUiState> = combine(
        fetchAllPlaylistFlowUseCase(),
        fetchDownloadTargetPlaylistsFlowUseCase(),
        isReorderBottomSheetVisible,
    ) { playlistList, downloadTarget, isReorderVisible ->
        PlaylistUiState(
            playlists = playlistList.map { it.toUiModel(isDownloadTarget = it.id == downloadTarget.targetId) },
            isReorderBottomSheetVisible = isReorderVisible,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistUiState())

    fun sendIntent(intent: PlaylistIntent) {
        when (intent) {
            is PlaylistIntent.OnClickPlaylistItem -> navigateToPlaylistDetailScreen(intent.playlistInfo)
            is PlaylistIntent.MakeNewPlaylist -> makeNewPlaylist(intent.title)
            is PlaylistIntent.OnClickDeletePlaylist -> deletePlaylist(intent.data.id)
            is PlaylistIntent.OnClickModifyPlaylist -> navigateToPlaylistEditScreen(intent.data)
            is PlaylistIntent.OnClickAddPlaylist -> navigateToAddPlaylistScreen(intent.data)
            is PlaylistIntent.OnClickShowReorderBottomSheet -> isReorderBottomSheetVisible.value = true
            is PlaylistIntent.OnDismissReorderBottomSheet -> isReorderBottomSheetVisible.value = false
            is PlaylistIntent.OnReorderPlaylist -> reorderPlaylist(intent.orderedIds)
            is PlaylistIntent.OnClickSetDownloadTarget -> setDownloadTarget(intent.data)
        }
    }

    private fun setDownloadTarget(data: PlaylistItemUiState) {
        viewModelScope.launch {
            val message = updatePlaylistIdOnDownloadUseCase(data.id).fold(
                onSuccess = { "이제 다운로드한 곡은 '${data.name}'에 담깁니다." },
                onFailure = { "기본 다운로드 플레이리스트를 바꾸지 못했습니다. 잠시 뒤 다시 시도해 주세요." },
            )
            eventChannel.send(PlaylistEvent.ShowToast(message))
        }
    }

    private fun reorderPlaylist(orderedIds: List<Int>) {
        viewModelScope.launch {
            updatePlaylistSequenceUseCase(orderedIds).onFailure {
                // 쓰기가 실패하면 DB 가 그대로라 Flow 가 재방출되지 않는다. 말해 주지 않으면
                // 시트는 새 순서를, 격자는 옛 순서를 동시에 보여준다.
                eventChannel.send(PlaylistEvent.ShowToast("순서를 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }

    private fun navigateToPlaylistDetailScreen(playlistInfo: PlaylistItemUiState) {
        viewModelScope.launch {
            eventChannel.send(PlaylistEvent.NavigateToPlaylistDetailScreen(playlistInfo))
        }
    }

    private fun navigateToPlaylistEditScreen(playlistInfo: PlaylistItemUiState) {
        viewModelScope.launch {
            eventChannel.send(PlaylistEvent.NavigateToPlaylistEditScreen(playlistInfo.id))
        }
    }

    private fun navigateToAddPlaylistScreen(data: PlaylistItemUiState) {
        viewModelScope.launch {
            eventChannel.send(PlaylistEvent.NavigateToPlaylistAddScreen(data.id))
        }
    }

    private fun makeNewPlaylist(title: String) {
        viewModelScope.launch {
            addNewPlaylistUseCase(title).onFailure {
                eventChannel.send(PlaylistEvent.ShowToast("플레이리스트를 만들지 못했습니다. 저장 공간을 확인하고 다시 시도해 주세요."))
            }
        }
    }

    private fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId).onFailure {
                eventChannel.send(PlaylistEvent.ShowToast("플레이리스트를 지우지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }
}
