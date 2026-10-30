package com.kintmin.presentation.ui.audio_media_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_edit.usecase.RevertAudioEditUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.karaoke.usecase.DeleteAudioMediaKaraokeNumberUseCase
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.model.MediaControlData
import com.kintmin.presentation.ui.audio_media_detail.navigation.AudioMediaDetailScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioMediaDetailViewModel constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
    private val deleteAudioMediaKaraokeNumberUseCase: DeleteAudioMediaKaraokeNumberUseCase,
    private val revertAudioEditUseCase: RevertAudioEditUseCase,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<AudioMediaDetailScreenRoute>().audioMediaId

    private val _eventFlow = MutableSharedFlow<AudioMediaDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    val data: StateFlow<AudioMediaDetailUiState> = fetchAudioMediaDetailFlowUseCase(audioMediaId)
        .map { it.toAudioMediaDetailUiState() }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AudioMediaDetailUiState(
                audioMediaId = audioMediaId,
                imageFileFullPath = null,
                audioMediaName = "",
                artist = "",
                playTime = "",
                audioDurationSeconds = null,
                audioMediaCreationTime = "",
                source = "",
                webViewUrl = null,
                audioMediaDescription = "",
                hasLyrics = false,
                tjKaraokeNumber = null,
                hasEditedAudio = false,
                playlists = listOf(),
            )
        )

    fun sendIntent(intent: AudioMediaDetailIntent) {
        when (intent) {
            AudioMediaDetailIntent.OnClickDeleteAudioMedia -> deleteAudioMedia()
            AudioMediaDetailIntent.OnClickUnlinkKaraokeNumber -> unlinkKaraokeNumber()
            AudioMediaDetailIntent.OnClickRevertAudioEdit -> revertAudioEdit()
        }
    }

    private fun deleteAudioMedia() {
        viewModelScope.launch {
            deleteAudioMediaUseCase(audioMediaId, data.value.source)
            triggerEvent(AudioMediaDetailEvent.OnNavigateToBack)
        }
    }

    private fun unlinkKaraokeNumber() {
        viewModelScope.launch {
            deleteAudioMediaKaraokeNumberUseCase(audioMediaId)
        }
    }

    private fun revertAudioEdit() {
        viewModelScope.launch {
            revertAudioEditUseCase(audioMediaId).onSuccess { audioMedia ->
                // 재생 큐는 같은 플레이리스트를 다시 재생할 때 갱신되지 않아 직접 교체해야 한다
                mediaControllerManager.tryReplaceMediaItem(
                    mediaId = audioMedia.id,
                    mediaItem = audioMedia.toMediaControlData(),
                )
            }
        }
    }

    private fun triggerEvent(newEvent: AudioMediaDetailEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}

private fun AudioMedia.toMediaControlData() = MediaControlData(
    mediaId = id.toString(),
    mediaFileUri = playbackFileFullPath,
    mediaTitle = name,
    mediaDescription = description,
    mediaArtist = artist,
    mediaDurationMs = playbackDuration?.inWholeMilliseconds ?: 0L,
    mediaArtworkFileUri = imageFileFullPath,
)
