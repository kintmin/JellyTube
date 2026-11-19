package com.kintmin.presentation.ui.main.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist.usecase.DeletePlaylistUseCase
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.domain.playlist.usecase.UpdatePlaylistSequenceUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel constructor(
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val updatePlaylistSequenceUseCase: UpdatePlaylistSequenceUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<PlaylistEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _isReorderBottomSheetVisible = MutableStateFlow(false)
    val isReorderBottomSheetVisible: StateFlow<Boolean> = _isReorderBottomSheetVisible.asStateFlow()

    val playlistFlow: StateFlow<List<PlaylistItemUiState>> = fetchAllPlaylistFlowUseCase()
        .map { playlistList ->
            // 미분류·즐겨찾기는 비어있으면(음원수 0) 목록에 표시하지 않는다.
            playlistList.filterNot {
                (it.type == PlaylistType.UNCATEGORIZED || it.type == PlaylistType.FAVORITE) &&
                    it.audioMediaCount == 0
            }.map { it.toUiModel() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistIntent) {
        when (intent) {
            is PlaylistIntent.OnClickPlaylistItem -> navigateToPlaylistDetailScreen(intent.playlistInfo)
            is PlaylistIntent.MakeNewPlaylist -> makeNewPlaylist(intent.title)
            is PlaylistIntent.OnClickDeletePlaylist -> deletePlaylist(intent.data.id)
            is PlaylistIntent.OnClickModifyPlaylist -> navigateToPlaylistEditScreen(intent.data)
            is PlaylistIntent.OnClickAddPlaylist -> navigateToAddPlaylistScreen(intent.data)
            is PlaylistIntent.OnClickShowReorderBottomSheet -> _isReorderBottomSheetVisible.value = true
            is PlaylistIntent.OnDismissReorderBottomSheet -> _isReorderBottomSheetVisible.value = false
            is PlaylistIntent.OnReorderPlaylist -> reorderPlaylist(intent.orderedIds)
        }
    }

    private fun reorderPlaylist(orderedIds: List<Int>) {
        viewModelScope.launch {
            updatePlaylistSequenceUseCase(orderedIds).onFailure {
                // 쓰기가 실패하면 DB 가 그대로라 Flow 가 재방출되지 않는다. 말해 주지 않으면
                // 시트는 새 순서를, 격자는 옛 순서를 동시에 보여준다.
                _eventFlow.emit(PlaylistEvent.ShowToast("순서를 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }

    private fun navigateToPlaylistDetailScreen(playlistInfo: PlaylistItemUiState) {
        viewModelScope.launch {
            _eventFlow.emit(PlaylistEvent.NavigateToPlaylistDetailScreen(playlistInfo))
        }
    }

    private fun navigateToPlaylistEditScreen(playlistInfo: PlaylistItemUiState) {
        viewModelScope.launch {
            _eventFlow.emit(PlaylistEvent.NavigateToPlaylistEditScreen(playlistInfo.id))
        }
    }

    private fun navigateToAddPlaylistScreen(data: PlaylistItemUiState) {
        viewModelScope.launch {
            _eventFlow.emit(PlaylistEvent.NavigateToPlaylistAddScreen(data.id))
        }
    }

    private fun makeNewPlaylist(title: String) {
        viewModelScope.launch {
            addNewPlaylistUseCase(title).onFailure {
                _eventFlow.emit(PlaylistEvent.ShowToast("플레이리스트를 만들지 못했습니다. 저장 공간을 확인하고 다시 시도해 주세요."))
            }
        }
    }

    private fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId).onFailure {
                _eventFlow.emit(PlaylistEvent.ShowToast("플레이리스트를 지우지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }
}
