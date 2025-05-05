package com.kintmin.presentation.ui.playlist_edit.list

sealed interface PlaylistEditListIntent {
    data class OnClickEditCheck(val data: PlaylistEditListItemUiState): PlaylistEditListIntent
    data class ReorderAudioItem(
        val reorderData: PlaylistEditListItemUiState,
        val targetData: PlaylistEditListItemUiState,
    ) : PlaylistEditListIntent
}