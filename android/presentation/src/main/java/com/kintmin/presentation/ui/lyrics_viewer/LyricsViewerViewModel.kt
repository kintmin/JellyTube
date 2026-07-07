package com.kintmin.presentation.ui.lyrics_viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.DeleteAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.GetAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.presentation.ui.lyrics_viewer.navigation.LyricsViewerScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsViewerViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAudioMediaDetailFlowUseCase: FetchAudioMediaDetailFlowUseCase,
    private val getAudioMediaLyricsUseCase: GetAudioMediaLyricsUseCase,
    private val parseLyricsUseCase: ParseLyricsUseCase,
    private val deleteAudioMediaLyricsUseCase: DeleteAudioMediaLyricsUseCase,
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsViewerScreenRoute>().audioMediaId

    private var parsedLines: List<LyricsLine> = emptyList()

    private val _eventFlow = MutableSharedFlow<LyricsViewerEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _data = MutableStateFlow(
        LyricsViewerUiState(
            title = "",
            lines = emptyList(),
            activeIndex = -1,
            isSynced = false,
            isLoading = true,
        )
    )
    val data = _data.asStateFlow()

    init {
        viewModelScope.launch {
            fetchAudioMediaDetailFlowUseCase(audioMediaId)
                .map { aggregates -> aggregates.firstOrNull()?.audioMedia }
                .distinctUntilChanged()
                .collect { audioMedia ->
                    val title = audioMedia?.name.orEmpty()
                    val lyricFileFullPath = audioMedia?.lyricFileFullPath
                    if (lyricFileFullPath == null) {
                        parsedLines = emptyList()
                        _data.update {
                            it.copy(title = title, lines = emptyList(), activeIndex = -1, isSynced = false, isLoading = false)
                        }
                    } else {
                        val rawLyrics = getAudioMediaLyricsUseCase(lyricFileFullPath).getOrNull().orEmpty()
                        parsedLines = parseLyricsUseCase(rawLyrics)
                        _data.update {
                            it.copy(
                                title = title,
                                lines = parsedLines.map { line -> line.text },
                                isSynced = parsedLines.any { line -> line.timeMs != null },
                                activeIndex = -1,
                                isLoading = false,
                            )
                        }
                    }
                }
        }
    }

    fun sendIntent(intent: LyricsViewerIntent) {
        when (intent) {
            LyricsViewerIntent.OnRefreshPosition -> refreshPosition()
            LyricsViewerIntent.OnClickDeleteLyrics -> deleteLyrics()
        }
    }

    private fun deleteLyrics() {
        viewModelScope.launch {
            val result = deleteAudioMediaLyricsUseCase(audioMediaId)
            if (result.isSuccess) {
                _eventFlow.emit(LyricsViewerEvent.ShowToast("가사를 삭제했습니다."))
                _eventFlow.emit(LyricsViewerEvent.NavigateToBack)
            } else {
                _eventFlow.emit(LyricsViewerEvent.ShowToast("가사 삭제에 실패했습니다."))
            }
        }
    }

    private fun refreshPosition() {
        if (parsedLines.isEmpty()) return

        // 이 화면의 음원이 실제로 재생 중일 때만 활성 줄을 계산한다.
        // (컨트롤러가 연결돼 있으면 미재생 상태에서도 currentPosition 이 0 을 반환해
        //  첫 줄 timeMs=0 이 잘못 하이라이트되는 것을 막는다.)
        val isThisMediaPlaying = mediaControllerManager.isPlaying &&
            mediaControllerManager.playingMediaItem?.mediaId == audioMediaId.toString()
        if (!isThisMediaPlaying) {
            if (_data.value.activeIndex != -1) {
                _data.update { it.copy(activeIndex = -1) }
            }
            return
        }

        val positionMs = mediaControllerManager.currentPosition ?: return
        val index = activeLyricIndex(parsedLines, positionMs)
        if (index != _data.value.activeIndex) {
            _data.update { it.copy(activeIndex = index) }
        }
    }
}
