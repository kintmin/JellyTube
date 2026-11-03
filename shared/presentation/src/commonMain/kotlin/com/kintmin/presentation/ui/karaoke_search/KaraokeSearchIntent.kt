package com.kintmin.presentation.ui.karaoke_search

sealed interface KaraokeSearchIntent {
    data class OnChangeQuery(val query: String) : KaraokeSearchIntent
    data object OnClickSearch : KaraokeSearchIntent
    data class OnClickResult(val item: KaraokeSearchUiState.KaraokeSearchItem) : KaraokeSearchIntent
}
