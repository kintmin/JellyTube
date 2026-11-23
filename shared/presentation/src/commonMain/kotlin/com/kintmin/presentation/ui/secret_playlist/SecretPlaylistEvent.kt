package com.kintmin.presentation.ui.secret_playlist

sealed interface SecretPlaylistEvent {
    /** [secretOnly] 면 그 플레이리스트에서 비밀 플레이리스트의 곡만 보여준다 — '찜' 입구다. */
    data class NavigateToPlaylistDetailScreen(
        val playlistId: Int,
        val secretOnly: Boolean = false,
    ) : SecretPlaylistEvent
    data class NavigateToPlaylistEditScreen(val playlistId: Int) : SecretPlaylistEvent
    data class NavigateToPlaylistAddScreen(val playlistId: Int, val playlistName: String) : SecretPlaylistEvent
    data class ShowToast(val message: String) : SecretPlaylistEvent
}
