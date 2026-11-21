package com.kintmin.presentation.navigation

sealed interface NavigationEvent {

    data object PopAll : NavigationEvent

    data object NavigateToMainPlaylistsTab : NavigationEvent

    data class NavigateToMainDownloadTab(
        val targetUrl: String?,
    ) : NavigationEvent

    data object NavigateToSettings : NavigationEvent
    data object NavigateToSettingAppLog : NavigationEvent
    data object NavigateToStep : NavigationEvent

    data object NavigateToPlayer : NavigationEvent

    data class NavigateToPlaylistContent(
        val playlistId: Int,
        val focusAudioMediaId: Int?,
    ) : NavigationEvent

    data class NavigateToAudioMedia(
        val audioMediaId: Int,
    ) : NavigationEvent

    data class NavigateToLyricsViewer(
        val audioMediaId: Int,
    ) : NavigationEvent
}
