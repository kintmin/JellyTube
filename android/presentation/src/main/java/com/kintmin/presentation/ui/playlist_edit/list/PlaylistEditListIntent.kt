package com.kintmin.presentation.ui.playlist_edit.list

sealed interface PlaylistEditListIntent {
    data class OnClickEditCheck(val data: PlaylistEditListItemUiState): PlaylistEditListIntent
    data class ReorderAudioItem(
        val reorderData: PlaylistEditListItemUiState,
        val targetData: PlaylistEditListItemUiState,
    ) : PlaylistEditListIntent

    data object OnClickClearCheckedItemList: PlaylistEditListIntent
    data object OnClickFullDeleteAudioMediaList: PlaylistEditListIntent
    data object OnClickDeleteAudioMediaListInPlaylist: PlaylistEditListIntent
    data class OnEditPlaylistTitle(val title: String): PlaylistEditListIntent
    data class OnEditPlaylistDescription(val description: String): PlaylistEditListIntent
}