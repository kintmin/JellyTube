package com.kintmin.presentation.ui.secret_playlist

sealed interface SecretPlaylistEvent {
    data class NavigateToPlaylistDetailScreen(val playlistId: Int) : SecretPlaylistEvent
    data class NavigateToPlaylistEditScreen(val playlistId: Int) : SecretPlaylistEvent
    data class NavigateToPlaylistAddScreen(val playlistId: Int, val playlistName: String) : SecretPlaylistEvent
    data class ShowToast(val message: String) : SecretPlaylistEvent
}
