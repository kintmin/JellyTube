package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.port.model.MediaControlData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlayerBarViewModel constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _currentMediaItem = MutableStateFlow(buildUiState(mediaControllerManager.playingMediaItem))

    val currentMediaItem = _currentMediaItem.asStateFlow()

    private var _isHandling = false

    fun sendIntent(intent: PlayerBarIntent) {
        when (intent) {
            PlayerBarIntent.OnClickPlayOrPauseButton -> {
                if (mediaControllerManager.isPlaying) {
                    mediaControllerManager.pause()
                } else {
                    mediaControllerManager.resume()
                }
                _currentMediaItem.update {
                    it.copy(isPlaying = mediaControllerManager.isPlaying)
                }
            }

            is PlayerBarIntent.OnChangeTimeSlider -> {
                _isHandling = true
                _currentMediaItem.update {
                    it.copy(currentDuration = intent.duration.toLong().seconds)
                }
            }

            PlayerBarIntent.OnChangeFinishTimeSlider -> {
                _isHandling = false
                mediaControllerManager.seek(currentMediaItem.value.currentDuration)
            }

            PlayerBarIntent.OnRefreshMediaData -> {
                val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.MILLISECONDS)

                if (!_isHandling) {
                    val playingMediaItem = mediaControllerManager.playingMediaItem
                    val isSameMedia = currentMediaItem.value.id == (playingMediaItem?.mediaId ?: "")
                    val isChangePlaying = mediaControllerManager.isPlaying != currentMediaItem.value.isPlaying || currentDuration == null
                    if (!isSameMedia) {
                        _currentMediaItem.update { buildUiState(playingMediaItem) }
                    } else if (isChangePlaying) {
                        _currentMediaItem.update {
                            it.copy(isPlaying = mediaControllerManager.isPlaying)
                        }
                    } else {
                        _currentMediaItem.update {
                            it.copy(currentDuration = currentDuration)
                        }
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
