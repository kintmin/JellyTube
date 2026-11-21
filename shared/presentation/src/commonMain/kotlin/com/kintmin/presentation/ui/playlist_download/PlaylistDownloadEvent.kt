package com.kintmin.presentation.ui.playlist_download

sealed interface PlaylistDownloadEvent {
    data object NavigateToBack : PlaylistDownloadEvent
    data class NavigateToPlaylist(val playlistId: Int) : PlaylistDownloadEvent
}
