package com.kintmin.presentation.ui.playlist_detail.header

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateIsPlaybackShufflingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackRepeatingUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.platform.util.MediaControllerManager
import com.kintmin.platform.util.model.MediaControlData
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.ui.playlist_detail.navigation.PlaylistDetailScreenRoute
import com.kintmin.presentation.util.Throttle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailHeaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaControllerManager: MediaControllerManager,
    fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
    fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    private val updatePlaybackRepeatingUseCase: UpdatePlaybackRepeatingUseCase,
    private val updateIsPlaybackShufflingUseCase: UpdateIsPlaybackShufflingUseCase,
    private val fetchAudioMediaListFlowUseCase: FetchAudioMediaListFlowUseCase,
) : ViewModel() {

    val playlistId = savedStateHandle.toRoute<PlaylistDetailScreenRoute>().playlistId

    private val _eventFlow = MutableSharedFlow<PlaylistDetailHeaderEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val isBasePlaylist get() = Playlist.isBasePlaylist(playlistId)

    val clickThrottle = Throttle(300L)

    val headerDataFlow: StateFlow<PlaylistDetailHeaderUiState> =
        combine(
            fetchPlaylistFlowUseCase(playlistId),
            fetchIsPlaybackRepeatingFlowUseCase(),
            fetchIsPlaybackShufflingFlowUseCase(),
        ) { playlist, isRepeating, isShuffling ->
            PlaylistDetailHeaderUiState(
                id = playlist.id,
                imageFileFullPath = playlist.imageFileFullPath,
                name = playlist.name,
                description = playlist.description,
                playlistSubtitle = "플레이리스트 · 음원수 ${playlist.audioMediaCount} · 재생시간 ${playlist.playTimeDuration.to_hh_colon_mm_colon_ss()}",
                isRepeating = isRepeating,
                isShuffling = isShuffling,
            )
        }.onEach {
            mediaControllerManager.setRepeatMode(it.isRepeating)
            mediaControllerManager.setShuffleMode(it.isShuffling)
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistDetailHeaderUiState(
                id = 0,
                imageFileFullPath = null,
                name = "",
                description = "",
                playlistSubtitle = "",
                isRepeating = false,
                isShuffling = false,
            )
        )


    fun sendIntent(intent: PlaylistDetailHeaderIntent) {
        viewModelScope.launch {
            clickThrottle {
                when (intent) {
                    PlaylistDetailHeaderIntent.OnClickAdd -> triggerEvent(PlaylistDetailHeaderEvent.NavigateToAddAudioMediaScreen)
                    PlaylistDetailHeaderIntent.OnClickEdit -> triggerEvent(PlaylistDetailHeaderEvent.NavigateToEditPlaylistScreen)
                    PlaylistDetailHeaderIntent.OnClickPlay -> playbackPlaylist()
                    PlaylistDetailHeaderIntent.OnClickRepeat -> updatePlaybackRepeating()
                    PlaylistDetailHeaderIntent.OnClickShuffle -> updateIsPlaybackShuffling()
                }
            }
        }
    }

    private fun playbackPlaylist() {
        viewModelScope.launch {
            val mediaControlDataList = fetchAudioMediaListFlowUseCase(playlistId).first().map {
                MediaControlData(
                    mediaId = it.audioMedia.id.toString(),
                    mediaFileUri = it.audioMedia.audioFileFullPath,
                    mediaTitle = it.audioMedia.name,
                    mediaDescription = it.audioMedia.description,
                    mediaArtist = it.audioMedia.artist,
                    mediaDurationMs = it.audioMedia.audioDuration?.inWholeMilliseconds ?: 0L,
                    mediaArtworkFileUri = it.audioMedia.imageFileFullPath,
                )
            }
            mediaControllerManager.playFromPlaylist(playlistId, null, mediaControlDataList)
        }
    }

    private fun updatePlaybackRepeating() {
        viewModelScope.launch {
            val newValue = !headerDataFlow.value.isRepeating
            updatePlaybackRepeatingUseCase(newValue)
        }
    }

    private fun updateIsPlaybackShuffling() {
        viewModelScope.launch {
            val newValue = !headerDataFlow.value.isShuffling
            updateIsPlaybackShufflingUseCase(newValue)
        }
    }

    private fun triggerEvent(newEvent: PlaylistDetailHeaderEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}