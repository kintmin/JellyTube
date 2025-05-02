package com.kintmin.presentation.ui.main.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.usecase.FetchPlaylistFlowUseCase
import com.kintmin.domain.usecase.FetchPlaylistListFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    val fetchPlaylistListFlowUseCase: FetchPlaylistListFlowUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<PlaylistEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val playlistFlow: StateFlow<List<PlaylistItemUiState>> = fetchPlaylistListFlowUseCase()
        .map { list -> list.map { it.toUiModel() } }
        .catch {
            // 에러처리
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistIntent) {
        when(intent) {
            PlaylistIntent.OnClickAddPlaylist -> TODO()
            is PlaylistIntent.OnClickPlaylistItem -> {
                viewModelScope.launch {
                    _eventFlow.emit(PlaylistEvent.NavigateToPlaylistDetailScreen(intent.playlistInfo))
                }
            }
        }
    }
}