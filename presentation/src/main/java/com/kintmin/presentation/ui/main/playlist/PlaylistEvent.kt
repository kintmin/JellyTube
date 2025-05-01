package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistEvent {
    data class NavigateToPlaylistDetailScreen(val playlistInfo: PlaylistItemUiState): PlaylistEvent

}