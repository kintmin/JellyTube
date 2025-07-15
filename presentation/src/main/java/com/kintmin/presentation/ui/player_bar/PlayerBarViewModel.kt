package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.platform.util.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerBarViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _currentMediaItem = MutableStateFlow(
        PlayerBarUiState(
            id = mediaControllerManager.playingMediaItem?.mediaId ?: "",
            title = (mediaControllerManager.playingMediaItem?.mediaMetadata?.title ?: "").toString(),
            currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
            playbackDuration = (mediaControllerManager.playingMediaItem?.mediaMetadata?.durationMs ?: 0L).milliseconds,
            imageFileFullPath = mediaControllerManager.playingMediaItem?.mediaMetadata?.artworkUri?.path,
            isPlaying = mediaControllerManager.isPlaying,
        )
    )
    val currentMediaItem = _currentMediaItem.asStateFlow()

    private var isHandling = false

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.MILLISECONDS)

                if (!isHandling) {
                    if (currentDuration == null) {
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
                                PlayerBarUiState(
                                    id = mediaControllerManager.playingMediaItem?.mediaId ?: "",
                                    title = (mediaControllerManager.playingMediaItem?.mediaMetadata?.title ?: "").toString(),
                                    currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                                    playbackDuration = (mediaControllerManager.playingMediaItem?.mediaMetadata?.durationMs ?: 0L).milliseconds,
                                    imageFileFullPath = mediaControllerManager.playingMediaItem?.mediaMetadata?.artworkUri?.path,
                                    mediaControllerManager.isPlaying,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun setHandling(newValue: Boolean) {
        isHandling = newValue
    }

    fun sendIntent(intent: PlayerBarIntent) {
        when (intent) {
            PlayerBarIntent.OnClickPlayOrPauseButton -> if (mediaControllerManager.isPlaying) {
                mediaControllerManager.pause()
            } else {
                mediaControllerManager.resume()
            }

            is PlayerBarIntent.OnChangeTimeSlider -> {
                _currentMediaItem.update {
                    it.copy(currentDuration = intent.duration.toLong().seconds)
                }
            }
        }
    }
}