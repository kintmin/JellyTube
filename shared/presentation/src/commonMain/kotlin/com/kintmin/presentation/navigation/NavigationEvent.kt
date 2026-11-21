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

    /** 비밀모드가 끝나 비밀 화면과 비밀 플레이리스트 화면을 닫는다. */
    data class LeaveSecretScreens(
        val secretPlaylistIds: Set<Int>,
    ) : NavigationEvent

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
