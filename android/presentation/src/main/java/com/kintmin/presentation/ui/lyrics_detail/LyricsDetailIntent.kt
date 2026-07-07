package com.kintmin.presentation.ui.lyrics_detail

sealed interface LyricsDetailIntent {
    data object OnClickApply : LyricsDetailIntent
}
