package com.kintmin.jellytube

sealed interface NavigationIntent {

    data object PopAll : NavigationIntent

    data object NavigateToMainPlaylistsTab : NavigationIntent

    data class NavigateToMainDownloadTab(
        val targetUrl: String?,
    ) : NavigationIntent

    data object NavigateToSettings : NavigationIntent
    data object NavigateToSettingAppLog : NavigationIntent
    data object NavigateToStep : NavigationIntent

    data object NavigateToPlayer : NavigationIntent

    data class NavigateToPlaylistContent(
        val playlistId: Int,
        val focusAudioMediaId: Int?,
    ) : NavigationIntent

    data class NavigateToAudioMedia(
        val audioMediaId: Int,
    ) : NavigationIntent

    data class NavigateToLyricsViewer(
        val audioMediaId: Int,
    ) : NavigationIntent
}
