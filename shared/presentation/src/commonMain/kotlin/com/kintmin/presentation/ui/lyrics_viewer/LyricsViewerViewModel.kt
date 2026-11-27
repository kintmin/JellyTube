package com.kintmin.presentation.ui.lyrics_viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.model.LyricsVariant
import com.kintmin.domain.lyrics.usecase.DeleteAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.DetectLyricsLanguageUseCase
import com.kintmin.domain.lyrics.usecase.GetLyricsVariantUseCase
import com.kintmin.domain.lyrics.usecase.GetPlaybackLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.presentation.port.StartLyricsVariantCreation
import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.ui.lyrics_viewer.navigation.LyricsViewerScreenRoute
import com.kintmin.presentation.util.NextLyricLineTimer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LyricsViewerViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val getPlaybackLyricsUseCase: GetPlaybackLyricsUseCase,
    private val deleteAudioMediaLyricsUseCase: DeleteAudioMediaLyricsUseCase,
    private val detectLyricsLanguageUseCase: DetectLyricsLanguageUseCase,
    private val getLyricsVariantUseCase: GetLyricsVariantUseCase,
    private val executeLyricsVariantCreation: StartLyricsVariantCreation,
    private val mediaControllerManager: PlaybackController,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsViewerScreenRoute>().audioMediaId

    private var parsedLines: List<LyricsLine> = emptyList()
    private var lyricFileFullPath: String? = null

    private val nextLyricLineTimer = NextLyricLineTimer(viewModelScope)

    private val eventChannel = Channel<LyricsViewerEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LyricsViewerEvent> = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(
        LyricsViewerUiState(
            title = "",
            lines = emptyList(),
            activeIndex = -1,
            isSynced = false,
            isLoading = true,
        )
    )
    val uiState: StateFlow<LyricsViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchAudioMediaDetailFlowUseCase(audioMediaId)
                .map { aggregates -> aggregates.firstOrNull()?.audioMedia }
                .distinctUntilChanged()
                .collect { audioMedia ->
                    val title = audioMedia?.name.orEmpty()
                    val path = audioMedia?.lyricFileFullPath
                    lyricFileFullPath = path
                    if (audioMedia == null || path == null) {
                        parsedLines = emptyList()
                        _uiState.update {
                            it.copy(
                                title = title,
                                lines = emptyList(),
                                activeIndex = -1,
                                isSynced = false,
                                isLoading = false,
                                translationLines = null,
                                transliterationLines = null,
                                showTranslateMenu = false,
                                showTransliterateMenu = false,
                            )
                        }
                    } else {
                        parsedLines = getPlaybackLyricsUseCase(audioMedia)

                        val language = detectLyricsLanguageUseCase(parsedLines)

                        val translationLines = getLyricsVariantUseCase(path, LyricsVariant.TRANSLATION)
                            .getOrNull()?.map { line -> line.text }
                        val transliterationLines = getLyricsVariantUseCase(path, LyricsVariant.TRANSLITERATION)
                            .getOrNull()?.map { line -> line.text }

                        _uiState.update {
                            it.copy(
                                title = title,
                                lines = parsedLines.map { line -> line.text },
                                isSynced = parsedLines.any { line -> line.timeMs != null },
                                activeIndex = -1,
                                isLoading = false,
                                translationLines = translationLines,
                                transliterationLines = transliterationLines,
                                showTranslateMenu = !language.hasKorean,
                                showTransliterateMenu = language.hasJapanese,
                                // 결과 파일이 바뀌어 들어왔으면 요청한 작업이 끝난 것이다.
                                isCreatingTranslation = it.isCreatingTranslation &&
                                    translationLines == it.translationLines,
                                isCreatingTransliteration = it.isCreatingTransliteration &&
                                    transliterationLines == it.transliterationLines,
                            )
                        }
                    }
                }
        }
    }

    fun sendIntent(intent: LyricsViewerIntent) {
        when (intent) {
            LyricsViewerIntent.OnRefreshPosition -> refreshPosition()
            is LyricsViewerIntent.OnClickLine -> seekToLine(intent.index)
            LyricsViewerIntent.OnClickDeleteLyrics -> deleteLyrics()
            LyricsViewerIntent.OnClickCreateTranslation -> createVariant(LyricsVariant.TRANSLATION)
            LyricsViewerIntent.OnClickCreateTransliteration -> createVariant(LyricsVariant.TRANSLITERATION)
        }
    }

    private fun seekToLine(index: Int) {
        if (!isThisMediaPlaying()) return
        val timeMs = parsedLines.getOrNull(index)?.timeMs ?: return
        mediaControllerManager.seek(timeMs.milliseconds)
        refreshPosition()
    }

    private fun createVariant(variant: LyricsVariant) {
        val path = lyricFileFullPath ?: return
        val isCreating = when (variant) {
            LyricsVariant.TRANSLATION -> _uiState.value.isCreatingTranslation
            LyricsVariant.TRANSLITERATION -> _uiState.value.isCreatingTransliteration
        }
        if (isCreating) return
        _uiState.update {
            when (variant) {
                LyricsVariant.TRANSLATION -> it.copy(isCreatingTranslation = true)
                LyricsVariant.TRANSLITERATION -> it.copy(isCreatingTransliteration = true)
            }
        }
        executeLyricsVariantCreation(
            audioMediaId = audioMediaId,
            lyricFileFullPath = path,
            variant = variant,
        )
        val message = when (variant) {
            LyricsVariant.TRANSLATION -> "번역을 시작했어요. 완료되면 알림으로 알려드릴게요."
            LyricsVariant.TRANSLITERATION -> "음차 번역을 시작했어요. 완료되면 알림으로 알려드릴게요."
        }
        viewModelScope.launch {
            eventChannel.send(LyricsViewerEvent.ShowToast(message))
        }
    }

    private fun deleteLyrics() {
        viewModelScope.launch {
            val result = deleteAudioMediaLyricsUseCase(audioMediaId)
            if (result.isSuccess) {
                eventChannel.send(LyricsViewerEvent.ShowToast("가사를 삭제했습니다."))
                eventChannel.send(LyricsViewerEvent.NavigateToBack)
            } else {
                eventChannel.send(LyricsViewerEvent.ShowToast("가사를 지우지 못했습니다. 잠시 뒤 다시 시도해 주세요."))
            }
        }
    }

    // 컨트롤러가 연결돼 있으면 미재생 상태에서도 currentPosition 이 0 을 반환한다.
    // 이 곡이 실제로 재생 중인지 먼저 보지 않으면 첫 줄 timeMs=0 이 잘못 하이라이트된다.
    private fun isThisMediaPlaying(): Boolean =
        mediaControllerManager.isPlaying &&
            mediaControllerManager.playingMediaItem?.mediaId == audioMediaId.toString()

    private fun refreshPosition() {
        nextLyricLineTimer.cancel()
        if (parsedLines.isEmpty()) return

        if (!isThisMediaPlaying()) {
            if (_uiState.value.activeIndex != -1 || _uiState.value.canSeek) {
                _uiState.update { it.copy(activeIndex = -1, canSeek = false) }
            }
            return
        }

        val positionMs = mediaControllerManager.currentPosition ?: return
        applyPosition(positionMs)
    }

    private fun applyPosition(positionMs: Long) {
        val index = activeLyricIndex(parsedLines, positionMs)
        val canSeek = _uiState.value.isSynced
        if (index != _uiState.value.activeIndex || canSeek != _uiState.value.canSeek) {
            _uiState.update { it.copy(activeIndex = index, canSeek = canSeek) }
        }
        nextLyricLineTimer.schedule(parsedLines, positionMs, mediaControllerManager.playbackSpeed) { reachedMs ->
            if (isThisMediaPlaying()) applyPosition(reachedMs)
        }
    }
}
