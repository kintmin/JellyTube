package com.kintmin.presentation.navigation

sealed interface NavigationEvent {

    data object PopAll : NavigationEvent

    data object NavigateToMainPlaylistsTab : NavigationEvent

    /** 플레이리스트 탭으로 가서 비밀 플레이리스트 입구(제목 길게 누르기)를 보여 준다. */
    data object HintSecretPlaylistEntry : NavigationEvent

    data class NavigateToMainDownloadTab(
        val targetUrl: String?,
    ) : NavigationEvent

    data object NavigateToSettings : NavigationEvent
    data object NavigateToSettingAppLog : NavigationEvent
    data object NavigateToStep : NavigationEvent

    data object NavigateToPlayer : NavigationEvent

    /** 비밀모드가 끝나 메인까지 걷어 낸다. 비밀 플레이리스트·비밀 곡 화면이 스택 어디에 있든 남지 않는다. */
    data object LeaveSecretScreens : NavigationEvent

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
