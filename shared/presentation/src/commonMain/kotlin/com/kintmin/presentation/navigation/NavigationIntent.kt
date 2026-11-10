package com.kintmin.presentation.navigation

/**
 * 최상위 화면 이동 명령. 앱 밖에서 들어온 주소를 [com.kintmin.presentation.deeplink.DeepLinkParser]
 * 가 이 목록으로 바꾸고, `MainNavHost` 가 순서대로 소비한다.
 */
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
