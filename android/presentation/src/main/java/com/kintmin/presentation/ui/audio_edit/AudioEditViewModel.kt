package com.kintmin.presentation.ui.audio_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.AudioWaveform
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.ExtractAudioWaveformUseCase
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioEditViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val extractAudioWaveformUseCase: ExtractAudioWaveformUseCase,
    private val applyAudioEditUseCase: ApplyAudioEditUseCase,
    private val saveEditedAudioAsNewMediaUseCase: SaveEditedAudioAsNewMediaUseCase,
    private val audioPreviewPlayer: AudioPreviewPlayer,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private companion object {

        /**
         * 파형 한 칸의 길이. 그릴 때 화면 폭에 맞춰 묶는다.
         * 최대 확대(화면 폭 1초)에서도 칸이 충분하고, 1시간짜리 음원도 7만 칸 정도라 가볍다.
         */
        const val WAVEFORM_BUCKET_MS = 50L
        const val MIN_WAVEFORM_BUCKET_COUNT = 400L
        const val MAX_WAVEFORM_BUCKET_COUNT = 100_000L

        /** 미리듣기 커서 갱신 주기. */
        const val PREVIEW_POLLING_INTERVAL_MS = 100L
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
                hasEditedAudio = audioMedia.editedAudioFileFullPath != null,
                // 싱크 가사(.lrc)만 편집으로 타임스탬프가 어긋난다
                hasSyncedLyrics = audioMedia.lyricFileFullPath?.endsWith(".lrc") == true,
                // 파형은 따로 채워지므로 음원 정보만 오면 바로 편집할 수 있다
                isLoading = false,
                // 저장 후 다시 흘러와도 편집 중인 타임라인은 유지한다. 원본 길이가 바뀔 때만 새로 시작한다.
                timeline = if (state.timeline.originDurationMs == originDurationMs) {
                    state.timeline
                } else {
                    AudioEditTimeline.initial(originDurationMs)
                },
            )
        }

        if (!isWaveformRequested && audioMedia.audioFileFullPath.isNotBlank()) {
            isWaveformRequested = true
            extractWaveform(
                audioFileFullPath = audioMedia.audioFileFullPath,
                bucketCount = (originDurationMs / WAVEFORM_BUCKET_MS)
                    .coerceIn(MIN_WAVEFORM_BUCKET_COUNT, MAX_WAVEFORM_BUCKET_COUNT)
                    .toInt(),
            )
        }
    }

    /** 긴 음원은 다 읽는 데 오래 걸려, 앞에서부터 읽힌 만큼씩 파형을 채운다. */
    private fun extractWaveform(audioFileFullPath: String, bucketCount: Int) {
        extractAudioWaveformUseCase(
            audioFileFullPath = audioFileFullPath,
            bucketCount = bucketCount,
        ).onEach { waveform ->
            _data.update { it.copy(waveform = waveform) }
        }.catch {
            // 파형이 없어도 시간으로 편집할 수 있으므로 화면은 계속 쓸 수 있게 둔다
            _data.update { it.copy(waveform = AudioWaveform.EMPTY) }
            _eventFlow.emit(AudioEditEvent.ShowToast("파형을 그리지 못했습니다. 시간으로 편집할 수 있습니다."))
        }.launchIn(viewModelScope)
    }

    /**
     * 미리듣기선 갱신. 플레이어는 위치를 흘려보내지 않아 주기적으로 읽는다.
     * 재생이 끝나면 isPlaying이 false가 되어 미리듣기선도 사라진다. 기준선은 건드리지 않는다.
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
            is AudioEditIntent.OnBaselineChanged -> updateBaseline(intent.ms)
            is AudioEditIntent.OnTimelineEndChanged -> updateTimeline { it.withEnd(intent.ms) }
            is AudioEditIntent.OnClipSelected -> _data.update { it.copy(selectedClipIndex = intent.index) }
            is AudioEditIntent.OnClipEdgeDragged -> updateTimeline {
                it.resizeEdge(index = intent.index, edge = intent.edge, toMs = intent.ms)
            }
            is AudioEditIntent.OnClipMoved -> updateTimeline {
                it.move(index = intent.index, toPositionMs = intent.positionMs)
            }
            AudioEditIntent.OnClickSplit -> splitAtBaseline()
            AudioEditIntent.OnClickMerge -> mergeAtBaseline()
            AudioEditIntent.OnClickDeleteClip -> deleteSelectedClip()
            AudioEditIntent.OnClickTogglePreview -> togglePreview()
            AudioEditIntent.OnConfirmOverwrite -> overwrite()
            is AudioEditIntent.OnConfirmSaveAsNew -> saveAsNewMedia(intent.name)
        }
    }

    private fun updateBaseline(ms: Long) {
        _data.update { state ->
            state.copy(baselineMs = ms.coerceIn(0L, state.timeline.maxEndMs))
        }
    }

    private fun splitAtBaseline() {
        val state = _data.value
        if (!state.canSplit) return

        updateTimeline { it.split(state.baselineMs) }
        // 나누면 조각 index가 밀려 이전 선택이 다른 조각을 가리킬 수 있다
        _data.update { it.copy(selectedClipIndex = null) }
    }

    private fun mergeAtBaseline() {
        val state = _data.value
        if (!state.canMerge) return

        updateTimeline { it.merge(state.baselineMs) }
        // 합치면 조각 index가 당겨져 이전 선택이 다른 조각을 가리킬 수 있다
        _data.update { it.copy(selectedClipIndex = null) }
    }

    private fun deleteSelectedClip() {
        val index = _data.value.selectedClipIndex ?: return

        updateTimeline { it.delete(index) }
        _data.update { it.copy(selectedClipIndex = null) }
    }

    /** 타임라인이 바뀌면 미리듣기는 옛 조각을 재생하고 있으므로 멈춘다. */
    private fun updateTimeline(transform: (AudioEditTimeline) -> AudioEditTimeline) {
        audioPreviewPlayer.pause()
        _data.update { state -> state.copy(timeline = transform(state.timeline)) }
    }

    private fun togglePreview() {
        if (audioPreviewPlayer.isPlaying) {
            audioPreviewPlayer.pause()
            return
        }

        val state = _data.value
        val parts = state.timeline.toParts()
        if (parts.isEmpty()) return

        audioPreviewPlayer.play(
            audioFileFullPath = state.originAudioFileFullPath,
            parts = parts,
            // 기준선이 결과물 끝 뒤에 있으면 들을 것이 없으니 처음부터 들려준다
            startAtMs = state.baselineMs.takeIf { it < state.timeline.endMs } ?: 0L,
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
                parts = state.timeline.toParts(),
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

    private fun saveAsNewMedia(name: String) {
        val state = _data.value
        if (!state.canSave) return

        audioPreviewPlayer.pause()
        _data.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveEditedAudioAsNewMediaUseCase(
                audioMediaId = audioMediaId,
                parts = state.timeline.toParts(),
                name = name,
            ).onSuccess {
                // 새 음원은 재생 큐에 없으므로 교체할 것이 없다(다운로드 경로와 동일하다)
                _data.update { it.copy(isSaving = false) }
                _eventFlow.emit(AudioEditEvent.ShowToast("편집한 음원을 새로 저장했습니다."))
                _eventFlow.emit(AudioEditEvent.NavigateToBack)
            }.onFailure { throwable ->
                _data.update { it.copy(isSaving = false) }
                _eventFlow.emit(
                    AudioEditEvent.ShowToast(throwable.message ?: "음원 저장에 실패했습니다.")
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
