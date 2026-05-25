package com.kintmin.presentation.ui.player_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackRepeatingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchIsPlaybackShufflingFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackPitchSemitoneFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.FetchPlaybackSpeedFlowUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdateIsPlaybackShufflingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackPitchSemitoneUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackRepeatingUseCase
import com.kintmin.domain.audio_play_setting.usecase.UpdatePlaybackSpeedUseCase
import com.kintmin.domain.playlist.usecase.FetchPlaylistFlowUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
    fetchIsPlaybackRepeatingFlowUseCase: FetchIsPlaybackRepeatingFlowUseCase,
    fetchIsPlaybackShufflingFlowUseCase: FetchIsPlaybackShufflingFlowUseCase,
    fetchPlaybackSpeedFlowUseCase: FetchPlaybackSpeedFlowUseCase,
    fetchPlaybackPitchSemitoneFlowUseCase: FetchPlaybackPitchSemitoneFlowUseCase,
    private val updatePlaybackRepeatingUseCase: UpdatePlaybackRepeatingUseCase,
    private val updateIsPlaybackShufflingUseCase: UpdateIsPlaybackShufflingUseCase,
    private val updatePlaybackSpeedUseCase: UpdatePlaybackSpeedUseCase,
    private val updatePlaybackPitchSemitoneUseCase: UpdatePlaybackPitchSemitoneUseCase,
    private val fetchPlaylistFlowUseCase: FetchPlaylistFlowUseCase,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<PlayerDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _data = mediaControllerManager.playingMediaItem.let {
        MutableStateFlow(
            PlayerDetailUiState(
                id = it?.mediaId ?: "",
                playlistId = mediaControllerManager.currentPlaylistId,
                playlistName = "",
                title = it?.mediaTitle ?: "",
                artist = it?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (it?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = it?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
                isShuffling = false,
                isRepeating = false,
                playbackSpeed = mediaControllerManager.playbackSpeed,
                playbackPitchSemitone = mediaControllerManager.playbackPitchSemitone,
            )
        )
    }
    val data = _data.asStateFlow()

    private var isHandlingSlider = false

    init {
        refreshPlaylistName(mediaControllerManager.currentPlaylistId)
        viewModelScope.launch {
            fetchIsPlaybackRepeatingFlowUseCase().collect {
                _data.update { prev -> prev.copy(isRepeating = it) }
                mediaControllerManager.setRepeatMode(it)
            }
        }
        viewModelScope.launch {
            fetchIsPlaybackShufflingFlowUseCase().collect {
                _data.update { prev -> prev.copy(isShuffling = it) }
                mediaControllerManager.setShuffleMode(it)
            }
        }
        viewModelScope.launch {
            fetchPlaybackSpeedFlowUseCase().collect { speed ->
                _data.update { prev -> prev.copy(playbackSpeed = speed) }
                mediaControllerManager.setPlaybackSpeed(speed)
            }
        }
        viewModelScope.launch {
            fetchPlaybackPitchSemitoneFlowUseCase().collect { semitone ->
                _data.update { prev -> prev.copy(playbackPitchSemitone = semitone) }
                mediaControllerManager.setPlaybackPitchSemitone(semitone)
            }
        }
    }

    fun sendIntent(intent: PlayerDetailIntent) {
        when (intent) {
            PlayerDetailIntent.OnClickPlayOrPauseButton -> {
                if (mediaControllerManager.isPlaying) {
                    mediaControllerManager.pause()
                } else {
                    mediaControllerManager.resume()
                }
                _data.update {
                    it.copy(isPlaying = mediaControllerManager.isPlaying)
                }
            }

            PlayerDetailIntent.OnClickPreviousMediaButton -> {
                mediaControllerManager.playPrevious()
                refreshMediaData()
            }

            PlayerDetailIntent.OnClickNextMediaButton -> {
                mediaControllerManager.playNext()
                refreshMediaData()
            }
            
            PlayerDetailIntent.OnClickShuffleButton -> {
                viewModelScope.launch {
                    val newValue = !data.value.isShuffling
                    updateIsPlaybackShufflingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickRepeatButton -> {
                viewModelScope.launch {
                    val newValue = !data.value.isRepeating
                    updatePlaybackRepeatingUseCase(newValue)
                }
            }

            PlayerDetailIntent.OnClickAddButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToAudioMediaEditScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickMoreButton -> {
                val mediaId = data.value.id.toIntOrNull()
                viewModelScope.launch {
                    if (mediaId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 음원 정보를 불러올 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToAudioMediaDetailScreen(mediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlayingPlaylistButton -> {
                viewModelScope.launch {
                    val playlistId = data.value.playlistId
                    val audioMediaId = data.value.id.toIntOrNull()
                    if (playlistId == null) {
                        _eventFlow.emit(PlayerDetailEvent.ShowToast("현재 재생 플레이리스트를 찾을 수 없습니다."))
                    } else {
                        _eventFlow.emit(PlayerDetailEvent.NavigateToPlayingPlaylist(playlistId, audioMediaId))
                    }
                }
            }

            PlayerDetailIntent.OnClickPlaybackSpeedButton -> {
                _data.update { it.copy(isPlaybackSpeedMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackSpeedMenu -> {
                _data.update { it.copy(isPlaybackSpeedMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackSpeed -> {
                viewModelScope.launch {
                    updatePlaybackSpeedUseCase(intent.speed)
                    _data.update {
                        it.copy(playbackSpeed = intent.speed)
                    }
                    mediaControllerManager.setPlaybackSpeed(intent.speed)
                }
            }

            PlayerDetailIntent.OnClickPlaybackPitchButton -> {
                _data.update { it.copy(isPlaybackPitchMenuVisible = true) }
            }

            PlayerDetailIntent.OnDismissPlaybackPitchMenu -> {
                _data.update { it.copy(isPlaybackPitchMenuVisible = false) }
            }

            is PlayerDetailIntent.OnSelectPlaybackPitchSemitone -> {
                viewModelScope.launch {
                    updatePlaybackPitchSemitoneUseCase(intent.semitone)
                    _data.update {
                        it.copy(playbackPitchSemitone = intent.semitone)
                    }
                    mediaControllerManager.setPlaybackPitchSemitone(intent.semitone)
                }
            }

            is PlayerDetailIntent.OnChangeTimeSlider -> {
                isHandlingSlider = true
                _data.update {
                    it.copy(currentDuration = intent.duration.toLong().seconds)
                }
            }

            PlayerDetailIntent.OnChangeFinishTimeSlider -> {
                isHandlingSlider = false
                mediaControllerManager.seek(data.value.currentDuration)
            }

            PlayerDetailIntent.OnRefreshMediaData -> refreshMediaData()
        }
    }

    private fun refreshMediaData() {
        if (isHandlingSlider) return

        val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.MILLISECONDS) ?: return
        val currentPlayingItem = mediaControllerManager.playingMediaItem
        val currentPlaylistId = mediaControllerManager.currentPlaylistId
        val isSamePlaylist = data.value.playlistId == currentPlaylistId
        val isSameMedia = data.value.id == currentPlayingItem?.mediaId

        if (isSameMedia && isSamePlaylist) {
            _data.update {
                it.copy(
                    currentDuration = currentDuration,
                    isPlaying = mediaControllerManager.isPlaying,
                )
            }
            return
        }

        _data.update {
            PlayerDetailUiState(
                id = currentPlayingItem?.mediaId ?: "",
                playlistId = currentPlaylistId,
                playlistName = if (isSamePlaylist) data.value.playlistName else "",
                title = currentPlayingItem?.mediaTitle ?: "",
                artist = currentPlayingItem?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (currentPlayingItem?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = currentPlayingItem?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
                isShuffling = data.value.isShuffling,
                isRepeating = data.value.isRepeating,
                playbackSpeed = data.value.playbackSpeed,
                playbackPitchSemitone = data.value.playbackPitchSemitone,
                isPlaybackSpeedMenuVisible = data.value.isPlaybackSpeedMenuVisible,
                isPlaybackPitchMenuVisible = data.value.isPlaybackPitchMenuVisible,
            )
        }

        if (!isSamePlaylist) {
            refreshPlaylistName(currentPlaylistId)
        }
    }

    private fun refreshPlaylistName(playlistId: Int?) {
        if (playlistId == null) {
            _data.update { it.copy(playlistName = "") }
            return
        }
        viewModelScope.launch {
            val name = runCatching {
                fetchPlaylistFlowUseCase(playlistId).first().name
            }.getOrDefault("")
            _data.update { it.copy(playlistName = name) }
        }
    }
}
