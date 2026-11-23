package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistIntent {
    data class OnClickPlaylistItem(val playlistInfo: PlaylistItemUiState) : PlaylistIntent
    data class MakeNewPlaylist(val title: String) : PlaylistIntent
    data class OnClickModifyPlaylist(val data: PlaylistItemUiState) : PlaylistIntent
    data class OnClickDeletePlaylist(val data: PlaylistItemUiState) : PlaylistIntent

    /** 담긴 곡의 파일까지 지운다. 비밀 플레이리스트에서만 고를 수 있다. */
    data class OnClickDeletePlaylistWithTracks(val data: PlaylistItemUiState) : PlaylistIntent
    data class OnClickAddPlaylist(val data: PlaylistItemUiState) : PlaylistIntent
    data class OnClickSetDownloadTarget(val data: PlaylistItemUiState) : PlaylistIntent
    data object OnClickShowReorderBottomSheet : PlaylistIntent
    data object OnDismissReorderBottomSheet : PlaylistIntent
    data class OnReorderPlaylist(val orderedIds: List<Int>) : PlaylistIntent
}
