package com.kintmin.presentation.ui.lyrics_viewer

sealed interface LyricsViewerEvent {
    data class ShowToast(val message: String) : LyricsViewerEvent
    data object NavigateToBack : LyricsViewerEvent
}
