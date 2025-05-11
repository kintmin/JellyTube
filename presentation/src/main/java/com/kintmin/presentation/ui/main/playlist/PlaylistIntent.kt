package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistIntent {
    data class OnClickPlaylistItem(val playlistInfo: PlaylistItemUiState) : PlaylistIntent
    data class MakeNewPlaylist(val title: String) : PlaylistIntent
    data class OnClickModifyPlaylist(val data: PlaylistItemUiState) : PlaylistIntent
    data class OnClickDeletePlaylist(val data: PlaylistItemUiState) : PlaylistIntent
    data class OnClickAddPlaylist(val data: PlaylistItemUiState) : PlaylistIntent
}
