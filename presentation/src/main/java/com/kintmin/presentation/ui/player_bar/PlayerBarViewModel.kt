package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import com.kintmin.platform.util.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerBarViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _currentMediaItem = mediaControllerManager.playingMediaItem.let {
        MutableStateFlow(
            PlayerBarUiState(
                id = it?.mediaId ?: "",
                title = it?.mediaTitle ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (it?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = it?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
            )
        )
    }

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
                    val isChangePlaying = mediaControllerManager.isPlaying != currentMediaItem.value.isPlaying || currentDuration == null
                    if (isChangePlaying) {
                        _currentMediaItem.update {
                            it.copy(isPlaying = mediaControllerManager.isPlaying)
                        }
                    } else {
                        val isSameMedia = currentMediaItem.value.id == mediaControllerManager.playingMediaItem?.mediaId
                        if (isSameMedia) {
                            _currentMediaItem.update {
                                it.copy(currentDuration = currentDuration)
                            }
                        } else {
                            _currentMediaItem.update {
                                mediaControllerManager.playingMediaItem.let {
                                    PlayerBarUiState(
                                        id = it?.mediaId ?: "",
                                        title = it?.mediaTitle ?: "",
                                        currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                                        playbackDuration = (it?.mediaDurationMs ?: 0L).milliseconds,
                                        imageFileFullPath = it?.mediaArtworkFileUri,
                                        isPlaying = mediaControllerManager.isPlaying,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}