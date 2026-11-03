package com.kintmin.presentation.ui.lyrics_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.presentation.ui.lyrics_detail.navigation.LyricsDetailScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsDetailViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val applyLyricsToAudioMediaUseCase: ApplyLyricsToAudioMediaUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LyricsDetailScreenRoute>()

    private val _data = MutableStateFlow(
        LyricsDetailUiState(
            trackName = route.trackName,
            artistName = route.artistName,
            plainLyrics = route.plainLyrics,
            syncedLyrics = route.syncedLyrics,
            isApplying = false,
        )
    )
    val data = _data.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LyricsDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun sendIntent(intent: LyricsDetailIntent) {
        when (intent) {
            LyricsDetailIntent.OnClickApply -> applyLyrics()
        }
    }

    private fun applyLyrics() {
        if (_data.value.isApplying) return
        viewModelScope.launch {
            _data.update { it.copy(isApplying = true) }
            val result = applyLyricsToAudioMediaUseCase(
                audioMediaId = route.audioMediaId,
                plainLyrics = route.plainLyrics.ifBlank { null },
                syncedLyrics = route.syncedLyrics.ifBlank { null },
            )
            _data.update { it.copy(isApplying = false) }
            if (result.isSuccess) {
                _eventFlow.emit(LyricsDetailEvent.ShowToast("가사를 적용했습니다."))
                _eventFlow.emit(LyricsDetailEvent.NavigateToBack)
            } else {
                _eventFlow.emit(LyricsDetailEvent.ShowToast("가사 적용에 실패했습니다."))
            }
        }
    }
}
