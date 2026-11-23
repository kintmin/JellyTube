package com.kintmin.presentation.ui.lyrics_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.ParseLyricsUseCase
import com.kintmin.presentation.ui.lyrics_detail.navigation.LyricsDetailScreenRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsDetailViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
    parseLyricsUseCase: ParseLyricsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LyricsDetailScreenRoute>()

    private val syncedLines = route.syncedLyrics
        .takeIf { it.isNotBlank() }
        ?.let(parseLyricsUseCase::invoke)
        .orEmpty()
        .mapNotNull { line ->
            line.timeMs?.let { timeMs -> LyricsDetailUiState.SyncedLine(timeMs, line.text) }
        }

    private val _uiState = MutableStateFlow(
        LyricsDetailUiState(
            trackName = route.trackName,
            artistName = route.artistName,
            plainLyrics = route.plainLyrics,
            isSynced = route.syncedLyrics.isNotBlank(),
            syncedLines = syncedLines,
            isApplying = false,
        )
    )
    val uiState: StateFlow<LyricsDetailUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<LyricsDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LyricsDetailEvent> = eventChannel.receiveAsFlow()

    fun sendIntent(intent: LyricsDetailIntent) {
        when (intent) {
            LyricsDetailIntent.OnClickApply -> applyLyrics()
        }
    }

    private fun applyLyrics() {
        if (_uiState.value.isApplying) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val result = applyLyricsToAudioMediaUseCase(
                audioMediaId = route.audioMediaId,
                plainLyrics = route.plainLyrics.ifBlank { null },
                syncedLyrics = route.syncedLyrics.ifBlank { null },
            )
            _uiState.update { it.copy(isApplying = false) }
            if (result.isSuccess) {
                eventChannel.send(LyricsDetailEvent.ShowToast("가사를 적용했습니다."))
                eventChannel.send(LyricsDetailEvent.NavigateToLyricsViewer(route.audioMediaId))
            } else {
                eventChannel.send(LyricsDetailEvent.ShowToast("가사 적용에 실패했습니다."))
            }
        }
    }
}
