package com.kintmin.presentation.ui.audio_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_edit.model.AudioEditSegment
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.BuildAudioEditSegmentsUseCase
import com.kintmin.domain.audio_edit.usecase.ExtractAudioWaveformUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.platform.audio_preview.AudioPreviewPlayer
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.platform.service_controller.model.MediaControlData
import com.kintmin.presentation.ui.audio_edit.navigation.AudioEditScreenRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioEditViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val extractAudioWaveformUseCase: ExtractAudioWaveformUseCase,
    private val buildAudioEditSegmentsUseCase: BuildAudioEditSegmentsUseCase,
    private val applyAudioEditUseCase: ApplyAudioEditUseCase,
    private val audioPreviewPlayer: AudioPreviewPlayer,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private companion object {

        /** 파형 버킷 수. 화면 폭보다 넉넉히 잡아두고 그릴 때 폭에 맞춰 묶는다. */
        const val WAVEFORM_BUCKET_COUNT = 400

        /** 미리듣기 커서 갱신 주기. */
        const val PREVIEW_POLLING_INTERVAL_MS = 100L

        /** 초기 선택 구간 길이. 앞부분을 자르는 경우가 가장 많아 앞쪽에 둔다. */
        const val INITIAL_SELECTION_MS = 10_000L
    }

    private val audioMediaId = savedStateHandle.toRoute<AudioEditScreenRoute>().audioMediaId

    private val _eventFlow = MutableSharedFlow<AudioEditEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _data = MutableStateFlow(AudioEditUiState.getEmpty(audioMediaId))
    val data = _data.asStateFlow()

    /** 파형은 원본 파일이 바뀌지 않는 한 한 번만 뽑는다. */
    private var isWaveformRequested = false

    init {
        collectAudioMedia()
        pollPreviewPosition()
    }

    private fun collectAudioMedia() {
        viewModelScope.launch {
            fetchAudioMediaDetailFlowUseCase(audioMediaId)
                .map { it.firstOrNull()?.audioMedia }
                .distinctUntilChanged()
                .collect { audioMedia ->
                    if (audioMedia == null) return@collect
                    applyAudioMedia(audioMedia)
                }
        }
    }

    private fun applyAudioMedia(audioMedia: AudioMedia) {
        val originDurationMs = audioMedia.audioDuration?.inWholeMilliseconds ?: 0L

        _data.update { state ->
            state.copy(
                audioMediaName = audioMedia.name,
                originAudioFileFullPath = audioMedia.audioFileFullPath,
                originDurationMs = originDurationMs,
                hasEditedAudio = audioMedia.editedAudioFileFullPath != null,
                // 싱크 가사(.lrc)만 편집으로 타임스탬프가 어긋난다
                hasSyncedLyrics = audioMedia.lyricFileFullPath?.endsWith(".lrc") == true,
                selectionStartMs = state.selectionStartMs,
                selectionEndMs = if (state.hasSelection) {
                    state.selectionEndMs
                } else {
                    minOf(INITIAL_SELECTION_MS, originDurationMs)
                },
            )
        }

        if (!isWaveformRequested && audioMedia.audioFileFullPath.isNotBlank()) {
            isWaveformRequested = true
            extractWaveform(audioMedia.audioFileFullPath)
        }
    }

    private fun extractWaveform(audioFileFullPath: String) {
        viewModelScope.launch {
            extractAudioWaveformUseCase(
                audioFileFullPath = audioFileFullPath,
                bucketCount = WAVEFORM_BUCKET_COUNT,
            ).onSuccess { waveform ->
                _data.update { it.copy(waveform = waveform, isLoading = false) }
            }.onFailure {
                // 파형이 없어도 시간으로 편집할 수 있으므로 화면은 계속 쓸 수 있게 둔다
                _data.update { it.copy(isLoading = false) }
                _eventFlow.emit(AudioEditEvent.ShowToast("파형을 그리지 못했습니다. 시간으로 편집할 수 있습니다."))
            }
        }
    }

    /**
     * 미리듣기 커서 갱신. 플레이어는 위치를 흘려보내지 않아 주기적으로 읽는다.
     * 재생이 끝나면 isPlaying이 false가 되어 커서도 사라진다.
     */
    private fun pollPreviewPosition() {
        viewModelScope.launch {
            while (true) {
                val isPlaying = audioPreviewPlayer.isPlaying
                _data.update { state ->
                    state.copy(
                        isPreviewPlaying = isPlaying,
                        previewPositionMs = audioPreviewPlayer.currentPositionMs.takeIf { isPlaying },
                    )
                }
                delay(PREVIEW_POLLING_INTERVAL_MS)
            }
        }
    }

    fun sendIntent(intent: AudioEditIntent) {
        when (intent) {
            is AudioEditIntent.OnSelectionChanged -> updateSelection(intent.startMs, intent.endMs)
            AudioEditIntent.OnClickCutSelection -> cutSelection()
            is AudioEditIntent.OnClickRestoreCut -> restoreCut(intent.index)
            AudioEditIntent.OnClickResetCuts -> updateCutRanges(emptyList())
            AudioEditIntent.OnClickTogglePreview -> togglePreview()
            AudioEditIntent.OnConfirmOverwrite -> overwrite()
        }
    }

    private fun updateSelection(startMs: Long, endMs: Long) {
        val durationMs = _data.value.originDurationMs
        _data.update { state ->
            state.copy(
                selectionStartMs = startMs.coerceIn(0, durationMs),
                selectionEndMs = endMs.coerceIn(0, durationMs),
            )
        }
    }

    private fun cutSelection() {
        val state = _data.value
        if (!state.hasSelection) return

        updateCutRanges(
            state.cutRanges + AudioEditSegment(
                startMs = state.selectionStartMs,
                endMs = state.selectionEndMs,
            )
        )
    }

    private fun restoreCut(index: Int) {
        val cutRanges = _data.value.cutRanges
        if (index !in cutRanges.indices) return

        updateCutRanges(cutRanges.filterIndexed { cutIndex, _ -> cutIndex != index })
    }

    /** 잘라낸 구간이 바뀌면 남길 구간을 다시 계산한다. 미리듣기는 옛 구간을 재생하므로 멈춘다. */
    private fun updateCutRanges(cutRanges: List<AudioEditSegment>) {
        audioPreviewPlayer.pause()

        _data.update { state ->
            state.copy(
                cutRanges = cutRanges,
                keepSegments = buildAudioEditSegmentsUseCase(
                    originDurationMs = state.originDurationMs,
                    cutRanges = cutRanges,
                ),
            )
        }
    }

    private fun togglePreview() {
        if (audioPreviewPlayer.isPlaying) {
            audioPreviewPlayer.pause()
            return
        }

        val state = _data.value
        // 아직 자른 게 없으면 원본 전체를 들려준다
        val segments = state.keepSegments.ifEmpty {
            listOf(AudioEditSegment(startMs = 0, endMs = state.originDurationMs))
        }
        if (segments.none { it.durationMs > 0 }) return

        audioPreviewPlayer.play(
            audioFileFullPath = state.originAudioFileFullPath,
            segments = segments,
        )
    }

    private fun overwrite() {
        val state = _data.value
        if (!state.canSave) return

        audioPreviewPlayer.pause()
        _data.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            applyAudioEditUseCase(
                audioMediaId = audioMediaId,
                segments = state.keepSegments,
            ).onSuccess { audioMedia ->
                // 재생 큐는 같은 플레이리스트를 다시 재생할 때 갱신되지 않는다. 직접 교체해야 편집본이 재생된다.
                mediaControllerManager.tryReplaceMediaItem(
                    mediaId = audioMedia.id,
                    mediaItem = audioMedia.toMediaControlData(),
                )
                _data.update { it.copy(isSaving = false) }
                _eventFlow.emit(AudioEditEvent.ShowToast("편집한 음원을 저장했습니다."))
                _eventFlow.emit(AudioEditEvent.NavigateToBack)
            }.onFailure { throwable ->
                _data.update { it.copy(isSaving = false) }
                _eventFlow.emit(
                    AudioEditEvent.ShowToast(throwable.message ?: "음원 편집에 실패했습니다.")
                )
            }
        }
    }

    override fun onCleared() {
        audioPreviewPlayer.release()
        super.onCleared()
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
