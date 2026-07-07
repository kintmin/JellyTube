package com.kintmin.presentation.ui.lyrics_edit

sealed interface LyricsEditIntent {
    data class OnChangeTime(val rowId: Int, val timeMs: Long) : LyricsEditIntent
    data class OnChangeText(val rowId: Int, val text: String) : LyricsEditIntent
    data class OnAddRowBelow(val rowId: Int) : LyricsEditIntent
    data class OnDeleteRow(val rowId: Int) : LyricsEditIntent
    data class OnReorder(val orderedIds: List<Int>) : LyricsEditIntent
    data object OnSplitByNewline : LyricsEditIntent
    data object OnClickSave : LyricsEditIntent
}
