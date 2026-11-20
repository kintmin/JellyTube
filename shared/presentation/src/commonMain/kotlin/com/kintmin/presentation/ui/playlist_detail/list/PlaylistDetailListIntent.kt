package com.kintmin.presentation.ui.playlist_detail.list

sealed interface PlaylistDetailListIntent {
    data class OnClickAudioItem(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickShowDetailAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickReorderAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnReorderAudioMedia(
        val reorderData: PlaylistDetailListItemUiState,
        val targetData: PlaylistDetailListItemUiState,
    ) : PlaylistDetailListIntent
    data class OnClickEditAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickRemoveFromPlaylist(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickDeleteAudioMediaFile(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnChangeSearchText(val searchText: String) : PlaylistDetailListIntent
}
