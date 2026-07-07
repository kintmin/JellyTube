package com.kintmin.presentation.ui.lyrics_search

sealed interface LyricsSearchIntent {
    data class OnChangeQuery(val query: String) : LyricsSearchIntent
    data object OnClickSearch : LyricsSearchIntent
    data class OnClickResult(val item: LyricsSearchUiState.LyricsSearchItem) : LyricsSearchIntent
}
