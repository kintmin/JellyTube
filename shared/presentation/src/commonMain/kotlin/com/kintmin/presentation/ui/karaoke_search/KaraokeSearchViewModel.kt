package com.kintmin.presentation.ui.karaoke_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.common_usecase.BuildSongSearchQueryUseCase
import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.domain.karaoke.usecase.ApplyKaraokeNumberToAudioMediaUseCase
import com.kintmin.domain.karaoke.usecase.SearchKaraokeUseCase
import com.kintmin.presentation.secret_mode.ExternalSearchConsent
import com.kintmin.presentation.ui.karaoke_search.navigation.KaraokeSearchScreenRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KaraokeSearchViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val searchKaraokeUseCase: SearchKaraokeUseCase,
    buildSongSearchQueryUseCase: BuildSongSearchQueryUseCase,
    private val applyKaraokeNumberToAudioMediaUseCase: ApplyKaraokeNumberToAudioMediaUseCase,
    private val externalSearchConsent: ExternalSearchConsent,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<KaraokeSearchScreenRoute>()
    private val audioMediaId = route.audioMediaId

    private val initialQuery = buildSongSearchQueryUseCase(route.initialQuery)

    private val _uiState = MutableStateFlow(
        // 비밀 곡인지 알아내기 전까지는 "결과 없음" 을 그리지 않는다.
        KaraokeSearchUiState(query = initialQuery, isLoading = true, results = emptyList())
    )
    val uiState: StateFlow<KaraokeSearchUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<KaraokeSearchEvent>(Channel.BUFFERED)
    val eventFlow: Flow<KaraokeSearchEvent> = eventChannel.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            externalSearchConsent.prepare(audioMediaId)
            startSearch(initialQuery)
        }
    }

    fun sendIntent(intent: KaraokeSearchIntent) {
        when (intent) {
            is KaraokeSearchIntent.OnChangeQuery ->
                _uiState.update { it.copy(query = intent.query) }

            KaraokeSearchIntent.OnClickSearch ->
                startSearch(_uiState.value.query)

            is KaraokeSearchIntent.OnClickResult ->
                viewModelScope.launch {
                    applyKaraokeNumberToAudioMediaUseCase(audioMediaId, intent.item.number)
                    eventChannel.send(KaraokeSearchEvent.NavigateBack)
                }

            KaraokeSearchIntent.OnConfirmExternalSearch -> {
                val query = externalSearchConsent.grant() ?: _uiState.value.query
                _uiState.update { it.copy(isConfirmingExternalSearch = false) }
                startSearch(query)
            }

            // 이 화면에는 검색 말고 할 일이 없어 고르지 않으면 돌아간다.
            KaraokeSearchIntent.OnDismissExternalSearch ->
                viewModelScope.launch {
                    _uiState.update { it.copy(isConfirmingExternalSearch = false) }
                    eventChannel.send(KaraokeSearchEvent.NavigateBack)
                }
        }
    }

    private fun startSearch(query: String) {
        if (externalSearchConsent.shouldAsk(query)) {
            _uiState.update { it.copy(isLoading = false, isConfirmingExternalSearch = true) }
            return
        }
        _uiState.update { it.copy(isConfirmingExternalSearch = false) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(isLoading = false, results = emptyList()) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        val results = searchKaraokeUseCase(query).getOrNull().orEmpty()
            .mapIndexed { index, song -> song.toItem(index) }
        _uiState.update { it.copy(isLoading = false, results = results, hasSearched = true) }
    }
}

private fun KaraokeSong.toItem(index: Int) = KaraokeSearchUiState.KaraokeSearchItem(
    id = index,
    number = number,
    title = title,
    singer = singer,
)
