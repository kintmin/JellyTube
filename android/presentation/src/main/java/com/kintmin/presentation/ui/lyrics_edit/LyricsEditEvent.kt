package com.kintmin.presentation.ui.lyrics_edit

sealed interface LyricsEditEvent {
    data class ShowToast(val message: String) : LyricsEditEvent
    data object NavigateToBack : LyricsEditEvent
}
