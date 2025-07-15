package com.kintmin.presentation.ui.player_bar

import androidx.lifecycle.ViewModel
import com.kintmin.platform.util.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PlayerBarViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _currentMediaItem = MutableStateFlow<PlayerBarUiState>(
        PlayerBarUiState(
            title = "",
            currentDuration = 0.seconds,
            playbackDuration = 200.seconds,
            imageFileFullPath = null,
        )
    )
    val currentMediaItem = _currentMediaItem.asStateFlow()

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