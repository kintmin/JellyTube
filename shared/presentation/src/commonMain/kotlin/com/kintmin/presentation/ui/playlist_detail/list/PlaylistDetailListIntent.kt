package com.kintmin.presentation.ui.playlist_detail.list

sealed interface PlaylistDetailListIntent {
    data class OnClickAudioItem(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickShowDetailAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickEditPlaylist(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnReorderAudioMedia(
        val reorderData: PlaylistDetailListItemUiState,
        val targetData: PlaylistDetailListItemUiState,
    ) : PlaylistDetailListIntent
    data class OnClickEditAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickRemoveFromPlaylist(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent

    /** 음원 파일까지 지운다. 모든 플레이리스트에서 사라진다. */
    data class OnClickDeleteAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnChangeSearchText(val searchText: String) : PlaylistDetailListIntent
}
