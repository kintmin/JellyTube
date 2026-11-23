package com.kintmin.presentation.ui.audio_media_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_edit.usecase.ObserveAudioEditSavingUseCase
import com.kintmin.domain.audio_edit.usecase.RevertAudioEditUseCase
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.karaoke.usecase.DeleteAudioMediaKaraokeNumberUseCase
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.toMediaControlData
import com.kintmin.presentation.ui.audio_media_detail.navigation.AudioMediaDetailScreenRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioMediaDetailViewModel constructor(
    savedStateHandle: SavedStateHandle,
    fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    observeAudioEditSavingUseCase: ObserveAudioEditSavingUseCase,
    private val deleteAudioMediaUseCase: DeleteAudioMediaUseCase,
    private val deleteAudioMediaKaraokeNumberUseCase: DeleteAudioMediaKaraokeNumberUseCase,
    private val revertAudioEditUseCase: RevertAudioEditUseCase,
    private val mediaControllerManager: PlaybackController,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<AudioMediaDetailScreenRoute>().audioMediaId

    private val eventChannel = Channel<AudioMediaDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<AudioMediaDetailEvent> = eventChannel.receiveAsFlow()

    private val isDeleting = MutableStateFlow(false)

    val uiState: StateFlow<AudioMediaDetailUiState> = combine(
        fetchAudioMediaDetailFlowUseCase(audioMediaId),
        observeAudioEditSavingUseCase(audioMediaId),
        isDeleting,
    ) { detail, isAudioEditSaving, isDeleting ->
        detail.toAudioMediaDetailUiState().copy(
            isAudioEditSaving = isAudioEditSaving,
            isDeleting = isDeleting,
        )
    }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), AudioMediaDetailUiState(
                audioMediaId = audioMediaId,
                imageFileFullPath = null,
                audioMediaName = "",
                artist = "",
                playTime = "",
                playbackDurationSeconds = null,
                audioDurationSeconds = null,
                audioMediaCreationTime = "",
                source = "",
                webViewUrl = null,
                audioMediaDescription = "",
                hasLyrics = false,
                tjKaraokeNumber = null,
                hasEditedAudio = false,
                playlists = listOf(),
                isLoaded = false,
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
        if (isDeleting.value) return

        isDeleting.value = true
        viewModelScope.launch {
            deleteAudioMediaUseCase(audioMediaId, uiState.value.source)
                .onSuccess { triggerEvent(AudioMediaDetailEvent.OnNavigateToBack) }
                .onFailure { throwable ->
                    // 실패했으면 화면에 남아야 한다. 떠나면 지워졌는지 아닌지 알 수 없다.
                    isDeleting.value = false
                    eventChannel.send(
                        AudioMediaDetailEvent.ShowToast(throwable.message ?: "음원을 삭제하지 못했습니다.")
                    )
                }
        }
    }

    private fun unlinkKaraokeNumber() {
        viewModelScope.launch {
            deleteAudioMediaKaraokeNumberUseCase(audioMediaId)
        }
    }

    private fun revertAudioEdit() {
        if (uiState.value.isAudioEditSaving) return

        viewModelScope.launch {
            revertAudioEditUseCase(audioMediaId).onSuccess { audioMedia ->
                // 재생 큐는 같은 플레이리스트를 다시 재생할 때 갱신되지 않아 직접 교체해야 한다
                mediaControllerManager.tryReplaceMediaItem(
                    mediaId = audioMedia.id,
                    mediaItem = audioMedia.toMediaControlData(),
                )
            }.onFailure { throwable ->
                eventChannel.send(AudioMediaDetailEvent.ShowToast(throwable.message ?: "원본으로 되돌리지 못했습니다."))
            }
        }
    }

    private fun triggerEvent(newEvent: AudioMediaDetailEvent) {
        viewModelScope.launch {
            eventChannel.send(newEvent)
        }
    }
}
