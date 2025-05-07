package com.kintmin.presentation.ui.main.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.usecase.DeletePlaylistUseCase
import com.kintmin.domain.usecase.FetchPlaylistListFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    fetchPlaylistListFlowUseCase: FetchPlaylistListFlowUseCase,
    private val addNewPlaylistUseCase: AddNewPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<PlaylistEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val playlistFlow: StateFlow<List<PlaylistItemUiState>> = fetchPlaylistListFlowUseCase()
        .map { list -> list.map { it.toUiModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistIntent) {
        when (intent) {
            is PlaylistIntent.OnClickPlaylistItem -> navigateToPlaylistDetailScreen(intent.playlistInfo)
            is PlaylistIntent.MakeNewPlaylist -> makeNewPlaylist(intent.title)
            is PlaylistIntent.OnClickDeletePlaylist -> deletePlaylist(intent.data.id)
            is PlaylistIntent.OnClickModifyPlaylist -> navigateToPlaylistEditScreen(intent.data)
            is PlaylistIntent.OnClickAddPlaylist -> navigateToAddPlaylistScreen(intent.data, intent.isBasePlaylist)
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

    private fun navigateToAddPlaylistScreen(data: PlaylistItemUiState, isBasePlaylist: Boolean) {
        viewModelScope.launch {
            if (isBasePlaylist) {
                _eventFlow.emit(PlaylistEvent.NavigateToMediaSearchScreen)
            } else {
                _eventFlow.emit(PlaylistEvent.NavigateToPlaylistAddScreen(data.id))
            }
        }
    }

    private fun makeNewPlaylist(title: String) {
        viewModelScope.launch {
            addNewPlaylistUseCase(title)
        }
    }

    private fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlistId)
        }
    }
}