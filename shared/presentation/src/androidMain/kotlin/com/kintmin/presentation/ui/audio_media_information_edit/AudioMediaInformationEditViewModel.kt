package com.kintmin.presentation.ui.audio_media_information_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.AddAudioMediaListToPlaylistUseCase
import com.kintmin.domain.audio_track.usecase.DeleteAudioTrackListUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.presentation.ui.audio_media_detail.navigation.AudioMediaDetailScreenRoute
import com.kintmin.presentation.util.Debounce
import com.kintmin.presentation.util.Throttle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioMediaInformationEditViewModel constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase,
    private val updateAudioMediaUseCase: UpdateAudioMediaUseCase,
    private val deleteAudioTrackListUseCase: DeleteAudioTrackListUseCase,
    private val addAudioMediaListToPlaylistUseCase: AddAudioMediaListToPlaylistUseCase,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<AudioMediaDetailScreenRoute>().audioMediaId

    private val nameDebounce = Debounce(200)
    private val artistDebounce = Debounce(200)
    private val descriptionDebounce = Debounce(200)

    private val deleteThrottle = Throttle(500)
    private val addThrottle = Throttle(500)
    private val isAddPlaylistBottomSheetVisible = MutableStateFlow(false)

    val data: StateFlow<AudioMediaInformationEditUiState> = combine(
        fetchAudioMediaDetailFlowUseCase(audioMediaId).map { it.toAudioMediaInformationEditUiState() },
        fetchAllPlaylistFlowUseCase(),
        isAddPlaylistBottomSheetVisible,
    ) { audioMedia, allPlaylists, isBottomSheetVisible ->
        val linkedPlaylistIds = audioMedia.playlists.map { it.playlistId }.toSet()
        audioMedia.copy(
            selectablePlaylists = allPlaylists
                .filterNot { playlist -> playlist.isBasePlaylist || playlist.id in linkedPlaylistIds }
                .map { playlist ->
                    AudioMediaInformationEditUiState.Playlist(
                        playlistId = playlist.id,
                        playlistName = playlist.name,
                        isBasePlaylist = playlist.isBasePlaylist,
                    )
                },
            isAddPlaylistBottomSheetVisible = isBottomSheetVisible,
        )
    }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AudioMediaInformationEditUiState(
                audioMediaId = audioMediaId,
                imageFileFullPath = null,
                audioMediaName = "",
                artist = "",
                playTime = "",
                audioMediaCreationTime = "",
                source = "",
                audioMediaDescription = "",
                playlists = listOf(),
            )
        )

    fun sendIntent(intent: AudioMediaInformationEditIntent) {
        when (intent) {
            is AudioMediaInformationEditIntent.OnAudioMediaArtistChanged -> updateAudioArtist(artist = intent.text)
            is AudioMediaInformationEditIntent.OnAudioMediaDescriptionChanged -> updateAudioDescription(description = intent.text)
            is AudioMediaInformationEditIntent.OnAudioMediaNameChanged -> updateAudioMediaName(name = intent.text)
            is AudioMediaInformationEditIntent.OnClickDeleteLinkedPlaylist -> deleteLinkedPlaylist(intent.playlistId)
            AudioMediaInformationEditIntent.OnClickShowAddPlaylistBottomSheet -> isAddPlaylistBottomSheetVisible.value = true
            AudioMediaInformationEditIntent.OnDismissAddPlaylistBottomSheet -> isAddPlaylistBottomSheetVisible.value = false
            is AudioMediaInformationEditIntent.OnClickAddLinkedPlaylist -> addLinkedPlaylist(intent.playlistId)
        }
    }

    private fun updateAudioMediaName(name: String) {
        viewModelScope.launch {
            nameDebounce {
                updateAudioMediaUseCase(
                    id = audioMediaId,
                    name = name,
                )
            }
        }
    }

    private fun updateAudioArtist(artist: String) {
        viewModelScope.launch {
            artistDebounce {
                updateAudioMediaUseCase(
                    id = audioMediaId,
                    artist = artist,
                )
            }
        }
    }

    private fun updateAudioDescription(description: String) {
        viewModelScope.launch {
            descriptionDebounce {
                updateAudioMediaUseCase(
                    id = audioMediaId,
                    description = description,
                )
            }
        }
    }

    private fun deleteLinkedPlaylist(playlistId: Int) {
        viewModelScope.launch {
            deleteThrottle {
                deleteAudioTrackListUseCase(playlistId, listOf(audioMediaId))
            }
        }
    }

    private fun addLinkedPlaylist(playlistId: Int) {
        viewModelScope.launch {
            addThrottle {
                addAudioMediaListToPlaylistUseCase(playlistId, listOf(audioMediaId))
                isAddPlaylistBottomSheetVisible.value = false
            }
        }
    }
}
