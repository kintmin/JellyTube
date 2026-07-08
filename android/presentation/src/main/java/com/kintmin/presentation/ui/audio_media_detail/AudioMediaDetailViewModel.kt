package com.kintmin.presentation.ui.audio_media_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.karaoke.usecase.DeleteAudioMediaKaraokeNumberUseCase
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
                audioMediaDescription = "",
                hasLyrics = false,
                tjKaraokeNumber = null,
                playlists = listOf(),
            )
        )

    fun sendIntent(intent: AudioMediaDetailIntent) {
        when (intent) {
            AudioMediaDetailIntent.OnClickDeleteAudioMedia -> deleteAudioMedia()
            AudioMediaDetailIntent.OnClickUnlinkKaraokeNumber -> unlinkKaraokeNumber()
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

    private fun triggerEvent(newEvent: AudioMediaDetailEvent) {
        viewModelScope.launch {
            _eventFlow.emit(newEvent)
        }
    }
}
