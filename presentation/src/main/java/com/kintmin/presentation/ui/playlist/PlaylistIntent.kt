package com.kintmin.presentation.ui.playlist

sealed interface PlaylistIntent {
    data object OnClickAddPlaylist: PlaylistIntent
    data class OnClickPlaylistItem(val playlistInfo: PlaylistItemUiState): PlaylistIntent
}
