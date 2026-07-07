package com.kintmin.presentation.ui.lyrics_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.ui.lyrics_search.navigation.LyricsSearchScreenRoute
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsSearchViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val searchLyricsUseCase: SearchLyricsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LyricsSearchScreenRoute>()
    private val audioMediaId = route.audioMediaId

    private val _data = MutableStateFlow(
        LyricsSearchUiState(query = route.initialQuery, isLoading = false, results = emptyList())
    )
    val data = _data.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LyricsSearchEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch { performSearch(route.initialQuery) }
    }

    fun sendIntent(intent: LyricsSearchIntent) {
        when (intent) {
            is LyricsSearchIntent.OnChangeQuery ->
                _data.update { it.copy(query = intent.query) }

            LyricsSearchIntent.OnClickSearch ->
                viewModelScope.launch { performSearch(_data.value.query) }

            is LyricsSearchIntent.OnClickResult ->
                viewModelScope.launch {
                    _eventFlow.emit(
                        LyricsSearchEvent.NavigateToLyricsDetail(
                            audioMediaId = audioMediaId,
                            trackName = intent.item.trackName,
                            artistName = intent.item.artistName,
                            plainLyrics = intent.item.plainLyrics,
                            syncedLyrics = intent.item.syncedLyrics,
                        )
                    )
                }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _data.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        _data.update { it.copy(isLoading = true) }
        val results = searchLyricsUseCase(query).getOrNull().orEmpty().map { it.toItem() }
        _data.update { it.copy(isLoading = false, results = results) }
    }
}

private fun LyricsSearchResult.toItem() = LyricsSearchUiState.LyricsSearchItem(
    id = id,
    trackName = trackName.orEmpty(),
    artistName = artistName.orEmpty(),
    albumName = albumName.orEmpty(),
    durationText = duration?.let { it.seconds.to_hh_colon_mm_colon_ss() }.orEmpty(),
    plainLyricsPreview = plainLyrics
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.take(80)
        .orEmpty(),
    plainLyrics = plainLyrics.orEmpty(),
    syncedLyrics = syncedLyrics.orEmpty(),
)
