package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistIntent {
    data class OnClickPlaylistItem(val playlistInfo: PlaylistItemUiState): PlaylistIntent
    data class MakeNewPlaylist(val title: String): PlaylistIntent
}
