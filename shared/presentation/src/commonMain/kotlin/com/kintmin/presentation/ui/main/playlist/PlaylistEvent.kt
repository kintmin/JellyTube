package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistEvent {
    data class NavigateToPlaylistDetailScreen(val playlistInfo: PlaylistItemUiState) : PlaylistEvent
    data class NavigateToPlaylistEditScreen(val playlistId: Int) : PlaylistEvent
    data class NavigateToPlaylistAddScreen(val playlistId: Int, val playlistName: String) : PlaylistEvent
    data class ShowToast(val message: String) : PlaylistEvent
}