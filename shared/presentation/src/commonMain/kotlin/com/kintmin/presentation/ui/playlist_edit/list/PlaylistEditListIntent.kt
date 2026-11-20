package com.kintmin.presentation.ui.playlist_edit.list

sealed interface PlaylistEditListIntent {
    data class OnClickEditCheck(val data: PlaylistEditListItemUiState): PlaylistEditListIntent
    data class ReorderAudioItem(
        val reorderData: PlaylistEditListItemUiState,
        val targetData: PlaylistEditListItemUiState,
    ) : PlaylistEditListIntent

    data class OnDragSelectItems(val checkedIds: Set<Int>): PlaylistEditListIntent
    data object OnClickClearCheckedItemList: PlaylistEditListIntent
    data object OnClickFullDeleteAudioMediaList: PlaylistEditListIntent
    data object OnClickDeleteAudioMediaListInPlaylist: PlaylistEditListIntent
    data class OnEditPlaylistTitle(val title: String): PlaylistEditListIntent
    data class OnEditPlaylistDescription(val description: String): PlaylistEditListIntent
    // ByteArray 는 내용으로 비교되지 않으므로 data class 로 두지 않는다.
    class OnPickPlaylistImage(val imageData: ByteArray): PlaylistEditListIntent
    data object OnClickResetPlaylistImage: PlaylistEditListIntent
}