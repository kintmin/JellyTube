package com.kintmin.presentation.ui.playlist

sealed interface PlaylistEvent {
    data class NavigateToPlaylistDetailScreen(val playlistInfo: PlaylistItemUiState): PlaylistEvent

}