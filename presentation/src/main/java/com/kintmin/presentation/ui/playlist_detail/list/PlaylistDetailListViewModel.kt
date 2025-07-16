package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistDetailListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val audioListFlow = fetchAudioMediaListFlowUseCase(playlistId).map { list -> list.map { it.toPlaylistDetailListItemUiState() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistDetailListIntent) {
        when (intent) {
            is PlaylistDetailListIntent.OnClickAudioItem -> playAudioMediaById(intent.data.id)
            is PlaylistDetailListIntent.OnClickShowDetailAudioMedia -> {
                triggerEvent(PlaylistDetailListEvent.NavigateToAudioDetailScreen(intent.data.id))
            }
        }
    }

    private fun playAudioMediaById(id: Int) {
        mediaControllerManager.playFromPlaylist(playlistId, id)
    }

    private fun triggerEvent(newEvent: PlaylistDetailListEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}