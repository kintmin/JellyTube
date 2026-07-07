package com.kintmin.presentation.ui.lyrics_viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.lyrics.model.LyricsLine
import com.kintmin.domain.lyrics.usecase.GetAudioMediaLyricsUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.domain.lyrics.usecase.activeLyricIndex
import com.kintmin.platform.service_controller.MediaControllerManager
import com.kintmin.presentation.ui.lyrics_viewer.navigation.LyricsViewerScreenRoute
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val mediaControllerManager: MediaControllerManager,
) : ViewModel() {

    private val audioMediaId = savedStateHandle.toRoute<LyricsViewerScreenRoute>().audioMediaId

    private var parsedLines: List<LyricsLine> = emptyList()

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
        }
    }

    private fun refreshPosition() {
        if (parsedLines.isEmpty()) return
        val positionMs = mediaControllerManager.currentPosition ?: return
        val index = activeLyricIndex(parsedLines, positionMs)
        if (index != _data.value.activeIndex) {
            _data.update { it.copy(activeIndex = index) }
        }
    }
}
