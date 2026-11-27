package com.kintmin.presentation.ui.lyrics_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.secret_mode.ExternalSearchConsent
import com.kintmin.presentation.ui.lyrics_search.navigation.LyricsSearchScreenRoute
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsSearchViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val searchLyricsUseCase: SearchLyricsUseCase,
    private val buildSongSearchQueryUseCase: BuildSongSearchQueryUseCase,
    private val sortLyricsSearchResultsUseCase: SortLyricsSearchResultsUseCase,
    private val externalSearchConsent: ExternalSearchConsent,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LyricsSearchScreenRoute>()
    private val audioMediaId = route.audioMediaId

    private val initialQuery = buildSongSearchQueryUseCase(route.initialQuery)

    private val _uiState = MutableStateFlow(
        LyricsSearchUiState(
            audioMediaName = route.initialQuery,
            playbackDurationText = route.playbackDurationSeconds
                ?.seconds
                ?.to_hh_colon_mm_colon_ss()
                .orEmpty(),
            query = initialQuery,
            // 비밀 곡인지 알아내기 전까지는 "결과 없음" 을 그리지 않는다.
            isLoading = true,
            results = emptyList(),
        )
    )
    val uiState: StateFlow<LyricsSearchUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<LyricsSearchEvent>(Channel.BUFFERED)
    val eventFlow: Flow<LyricsSearchEvent> = eventChannel.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            externalSearchConsent.prepare(audioMediaId)
            startSearch(initialQuery)
        }
    }

    fun sendIntent(intent: LyricsSearchIntent) {
        when (intent) {
            is LyricsSearchIntent.OnChangeQuery ->
                _uiState.update { it.copy(query = intent.query) }

            LyricsSearchIntent.OnClickSearch ->
                startSearch(_uiState.value.query)

            is LyricsSearchIntent.OnClickResult ->
                viewModelScope.launch {
                    eventChannel.send(
                        LyricsSearchEvent.NavigateToLyricsDetail(
                            audioMediaId = audioMediaId,
                            trackName = intent.item.trackName,
                            artistName = intent.item.artistName,
                            plainLyrics = intent.item.plainLyrics,
                            syncedLyrics = intent.item.syncedLyrics,
                        )
                    )
                }

            LyricsSearchIntent.OnClickAddLyricsManually ->
                viewModelScope.launch {
                    eventChannel.send(LyricsSearchEvent.NavigateToLyricsEdit(audioMediaId))
                }

            LyricsSearchIntent.OnConfirmExternalSearch -> {
                val query = externalSearchConsent.grant() ?: _uiState.value.query
                _uiState.update { it.copy(isConfirmingExternalSearch = false) }
                startSearch(query)
            }

            // 가사를 직접 넣는 길은 남아 있어 화면을 닫지 않는다.
            LyricsSearchIntent.OnDismissExternalSearch ->
                _uiState.update { it.copy(isConfirmingExternalSearch = false) }
        }
    }

    private fun startSearch(query: String) {
        if (externalSearchConsent.shouldAsk(query)) {
            _uiState.update { it.copy(isLoading = false, isConfirmingExternalSearch = true) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        val results = searchLyricsUseCase(query).getOrNull().orEmpty()
            .let { sortLyricsSearchResultsUseCase(it, query, route.durationSeconds) }
            .map { it.toItem() }
        _uiState.update { it.copy(isLoading = false, results = results) }
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
