package com.kintmin.presentation.ui.lyrics_detail

sealed interface LyricsDetailEvent {
    data class ShowToast(val message: String) : LyricsDetailEvent
    data object NavigateToBack : LyricsDetailEvent
}
