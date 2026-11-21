package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.MediaControlData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlayerBarViewModel constructor(
    private val mediaControllerManager: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildUiState(mediaControllerManager.playingMediaItem))

    val uiState: StateFlow<PlayerBarUiState> = _uiState.asStateFlow()

    fun sendIntent(intent: PlayerBarIntent) {
        when (intent) {
            PlayerBarIntent.OnClickPlayOrPauseButton -> {
                if (mediaControllerManager.isPlaying) {
                    mediaControllerManager.pause()
                } else {
                    mediaControllerManager.resume()
                }
                _uiState.update {
                    it.copy(isPlaying = mediaControllerManager.isPlaying)
                }
            }

            PlayerBarIntent.OnRefreshMediaData -> {
                val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.MILLISECONDS)

                val playingMediaItem = mediaControllerManager.playingMediaItem
                val isSameMedia = uiState.value.id == (playingMediaItem?.mediaId ?: "") &&
                    uiState.value.playbackDuration == (playingMediaItem?.mediaDurationMs ?: 0L).milliseconds
                val isChangePlaying = mediaControllerManager.isPlaying != uiState.value.isPlaying || currentDuration == null
                if (!isSameMedia) {
                    _uiState.update { buildUiState(playingMediaItem) }
                } else if (isChangePlaying) {
                    _uiState.update {
                        it.copy(isPlaying = mediaControllerManager.isPlaying)
                    }
                } else {
                    _uiState.update {
                        it.copy(currentDuration = currentDuration)
                    }
                }
            }
        }
    }

    private fun buildUiState(item: MediaControlData?) = PlayerBarUiState(
        id = item?.mediaId ?: "",
        title = item?.mediaTitle ?: "",
        currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
        playbackDuration = (item?.mediaDurationMs ?: 0L).milliseconds,
        imageFileFullPath = item?.mediaArtworkFileUri,
        isPlaying = mediaControllerManager.isPlaying,
    )
}
