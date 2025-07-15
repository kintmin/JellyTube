package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kintmin.platform.util.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerBarViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _currentMediaItem = MutableStateFlow(
        PlayerBarUiState(
            title = (mediaControllerManager.playingMediaItem?.mediaMetadata?.title ?: "").toString(),
            currentDuration = (mediaControllerManager.currentPosition ?: 0).seconds,
            playbackDuration = (mediaControllerManager.playingMediaItem?.mediaMetadata?.durationMs ?: 0L).seconds,
            imageFileFullPath = mediaControllerManager.playingMediaItem?.mediaMetadata?.artworkUri?.path,
        )
    )
    val currentMediaItem = _currentMediaItem.asStateFlow()

    init {
        viewModelScope.launch {
            delay(500)
            val currentDuration = mediaControllerManager.currentPosition?.toDuration(DurationUnit.SECONDS)

            val isHandling = false
            if (!isHandling) {
                if (currentDuration == null) {
                    // 버튼 일시정지
                } else {
                    if (_currentMediaItem.value.currentDuration < currentDuration) {
                        _currentMediaItem.update {
                            it.copy(currentDuration = currentDuration)
                        }
                    } else {
                        _currentMediaItem.update {
                            PlayerBarUiState(
                                title = (mediaControllerManager.playingMediaItem?.mediaMetadata?.title ?: "").toString(),
                                currentDuration = (mediaControllerManager.currentPosition ?: 0).seconds,
                                playbackDuration = (mediaControllerManager.playingMediaItem?.mediaMetadata?.durationMs ?: 0L).seconds,
                                imageFileFullPath = mediaControllerManager.playingMediaItem?.mediaMetadata?.artworkUri?.path,
                            )
                        }
                    }
                }
            }
        }
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