package com.kintmin.presentation.ui.playlist_detail.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.model.MediaControlData
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.util.Throttle
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
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistDetailListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val clickThrottle = Throttle(300L)

    val audioListFlow =
        fetchAudioMediaListFlowUseCase(playlistId).map { list -> list.map { it.toPlaylistDetailListItemUiState() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendIntent(intent: PlaylistDetailListIntent) {
        viewModelScope.launch {
            clickThrottle {
                when (intent) {
                    is PlaylistDetailListIntent.OnClickAudioItem -> playAudioMediaById(intent.data.id)
                    is PlaylistDetailListIntent.OnClickShowDetailAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioDetailScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickEditAudioMedia -> {
                        triggerEvent(PlaylistDetailListEvent.NavigateToAudioEditScreen(intent.data.id))
                    }
                    is PlaylistDetailListIntent.OnClickDeleteAudioMediaInPlaylist -> {
                        deleteAudioMedia(intent.data.id, intent.data.source)
                    }
                }
            }
        }
    }

    private fun deleteAudioMedia(audioMediaId: Int, source: String) {
        viewModelScope.launch {
            deleteAudioMediaUseCase(audioMediaId, source)
        }
    }

    private fun playAudioMediaById(id: Int) {
        viewModelScope.launch {
            val mediaControlDataList = audioListFlow.value.map {
                MediaControlData(
                    mediaId = it.id.toString(),
                    mediaFileUri = it.audioFileFullPath,
                    mediaTitle = it.mediaName,
                    mediaDescription = it.description,
                    mediaArtist = it.artist,
                    mediaDurationMs = it.audioDuration?.inWholeMilliseconds ?: 0L,
                    mediaArtworkFileUri = it.imageFileFullPath,
                )
            }
            mediaControllerManager.playFromPlaylist(playlistId, id, mediaControlDataList)
        }
    }

    private fun triggerEvent(newEvent: PlaylistDetailListEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}
