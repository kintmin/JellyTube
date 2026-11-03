package com.kintmin.presentation.ui.lyrics_search

sealed interface LyricsSearchEvent {
    data class NavigateToLyricsDetail(
        val audioMediaId: Int,
        val trackName: String,
        val artistName: String,
        val plainLyrics: String,
        val syncedLyrics: String,
    ) : LyricsSearchEvent

    data class NavigateToLyricsEdit(val audioMediaId: Int) : LyricsSearchEvent
}
