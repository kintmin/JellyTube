package com.kintmin.presentation.ui.player_detail

import androidx.lifecycle.ViewModel
import com.kintmin.platform.service_controller.MediaControllerManager
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
class PlayerDetailViewModel @Inject constructor(
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val _data = mediaControllerManager.playingMediaItem.let {
        MutableStateFlow(
            PlayerDetailUiState(
                id = it?.mediaId ?: "",
                title = it?.mediaTitle ?: "",
                artist = it?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (it?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = it?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
            )
        )
    }
    val data = _data.asStateFlow()

    private var isHandlingSlider = false

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
        val isSameMedia = data.value.id == currentPlayingItem?.mediaId

        if (isSameMedia) {
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
                title = currentPlayingItem?.mediaTitle ?: "",
                artist = currentPlayingItem?.mediaArtist ?: "",
                currentDuration = (mediaControllerManager.currentPosition ?: 0).milliseconds,
                playbackDuration = (currentPlayingItem?.mediaDurationMs ?: 0L).milliseconds,
                imageFileFullPath = currentPlayingItem?.mediaArtworkFileUri,
                isPlaying = mediaControllerManager.isPlaying,
            )
        }
    }
}
