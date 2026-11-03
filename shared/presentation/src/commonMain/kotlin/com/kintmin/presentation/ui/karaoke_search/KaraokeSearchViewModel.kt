package com.kintmin.presentation.ui.karaoke_search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kintmin.domain.karaoke.model.KaraokeSong
import com.kintmin.domain.karaoke.usecase.ApplyKaraokeNumberToAudioMediaUseCase
import com.kintmin.domain.karaoke.usecase.SearchKaraokeUseCase
import com.kintmin.presentation.ui.karaoke_search.navigation.KaraokeSearchScreenRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KaraokeSearchViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val searchKaraokeUseCase: SearchKaraokeUseCase,
    private val applyKaraokeNumberToAudioMediaUseCase: ApplyKaraokeNumberToAudioMediaUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<KaraokeSearchScreenRoute>()
    private val audioMediaId = route.audioMediaId

    private val _data = MutableStateFlow(
        KaraokeSearchUiState(query = route.initialQuery, isLoading = false, results = emptyList())
    )
    val data = _data.asStateFlow()

    private val _eventFlow = MutableSharedFlow<KaraokeSearchEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 진행 중인 검색 코루틴. 새 검색을 시작하기 전에 취소한다.
    private var searchJob: Job? = null

    init {
        startSearch(route.initialQuery)
    }

    fun sendIntent(intent: KaraokeSearchIntent) {
        when (intent) {
            is KaraokeSearchIntent.OnChangeQuery ->
                _data.update { it.copy(query = intent.query) }

            KaraokeSearchIntent.OnClickSearch ->
                startSearch(_data.value.query)

            is KaraokeSearchIntent.OnClickResult ->
                viewModelScope.launch {
                    applyKaraokeNumberToAudioMediaUseCase(audioMediaId, intent.item.number)
                    _eventFlow.emit(KaraokeSearchEvent.NavigateBack)
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
        val results = searchKaraokeUseCase(query).getOrNull().orEmpty()
            .mapIndexed { index, song -> song.toItem(index) }
        _data.update { it.copy(isLoading = false, results = results) }
    }
}

private fun KaraokeSong.toItem(index: Int) = KaraokeSearchUiState.KaraokeSearchItem(
    id = index,
    number = number,
    title = title,
    singer = singer,
)
