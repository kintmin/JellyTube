package com.kintmin.presentation.ui.lyrics_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.lyrics.model.LyricsSearchResult
import com.kintmin.domain.lyrics.usecase.BuildLyricsSearchQueryUseCase
import com.kintmin.domain.lyrics.usecase.SearchLyricsUseCase
import com.kintmin.domain.lyrics.usecase.SortLyricsSearchResultsUseCase
import com.kintmin.presentation.extension.to_hh_colon_mm_colon_ss
import com.kintmin.presentation.ui.lyrics_search.navigation.LyricsSearchScreenRoute
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LyricsSearchViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val searchLyricsUseCase: SearchLyricsUseCase,
    private val buildLyricsSearchQueryUseCase: BuildLyricsSearchQueryUseCase,
    private val sortLyricsSearchResultsUseCase: SortLyricsSearchResultsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LyricsSearchScreenRoute>()
    private val audioMediaId = route.audioMediaId

    // 제목 원문을 검색어 규칙(괄호 제거 + 3단어)으로 정제해 초기 쿼리로 쓴다.
    private val initialQuery = buildLyricsSearchQueryUseCase(route.initialQuery)

    private val _data = MutableStateFlow(
        LyricsSearchUiState(query = initialQuery, isLoading = false, results = emptyList())
    )
    val data = _data.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LyricsSearchEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 진행 중인 검색 코루틴. 새 검색을 시작하기 전에 취소한다.
    private var searchJob: Job? = null

    init {
        startSearch(initialQuery)
    }

    fun sendIntent(intent: LyricsSearchIntent) {
        when (intent) {
            is LyricsSearchIntent.OnChangeQuery ->
                _data.update { it.copy(query = intent.query) }

            LyricsSearchIntent.OnClickSearch ->
                startSearch(_data.value.query)

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

            LyricsSearchIntent.OnClickAddLyricsManually ->
                viewModelScope.launch {
                    _eventFlow.emit(LyricsSearchEvent.NavigateToLyricsEdit(audioMediaId))
                }
        }
    }

    // 이미 로딩 중인 검색이 있으면 취소하고, 현재 상태로 새 검색을 시작한다.
    private fun startSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _data.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        _data.update { it.copy(isLoading = true) }
        val results = searchLyricsUseCase(query).getOrNull().orEmpty()
            .let { sortLyricsSearchResultsUseCase(it, route.durationSeconds) }
            .map { it.toItem() }
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
