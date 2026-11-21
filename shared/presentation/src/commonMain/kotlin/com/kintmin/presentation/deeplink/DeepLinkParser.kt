package com.kintmin.presentation.deeplink

import com.kintmin.presentation.navigation.NavigationEvent

/**
 * 앱 밖에서 들어온 주소를 화면 이동 명령으로 바꾼다.
 * 순수 함수다 — 재생 상태도 포트가 아니라 파라미터로 받는다. 우리 주소가 아니면 빈 목록이다.
 */
object DeepLinkParser {

    /**
     * @param playingPlaylistId 플레이어 딥링크가 "재생 목록에서 들어왔다" 고 할 때만 쓴다.
     * @param playingMediaId 위와 같은 자리에서만 쓴다.
     */
    fun parse(
        url: String,
        playingPlaylistId: Int? = null,
        playingMediaId: Int? = null,
    ): List<NavigationEvent> {
        val scheme = UrlParts.scheme(url)
        if (scheme != DeepLinkConstants.SCHEME && scheme != DeepLinkConstants.APP_SCHEME) {
            return emptyList()
        }
        if (UrlParts.host(url) != DeepLinkConstants.HOST) return emptyList()

        val pathSegments = UrlParts.pathSegments(url)
        if (pathSegments.getOrNull(0) != DeepLinkConstants.Path.MAIN) return emptyList()

        val intents = mutableListOf<NavigationEvent>(NavigationEvent.PopAll)

        when (pathSegments.getOrNull(1)) {
            DeepLinkConstants.Path.SETTINGS -> {
                intents += NavigationEvent.NavigateToMainPlaylistsTab
                intents += NavigationEvent.NavigateToSettings

                if (pathSegments.getOrNull(2) == DeepLinkConstants.Path.APP_LOG) {
                    intents += NavigationEvent.NavigateToSettingAppLog
                }
            }

            DeepLinkConstants.Path.STEP -> {
                intents += NavigationEvent.NavigateToMainPlaylistsTab
                intents += NavigationEvent.NavigateToSettings
                intents += NavigationEvent.NavigateToStep
            }

            DeepLinkConstants.Path.DOWNLOAD -> {
                intents += NavigationEvent.NavigateToMainDownloadTab(targetUrlOf(url))
            }

            DeepLinkConstants.Path.PLAYER -> {
                intents += NavigationEvent.NavigateToMainPlaylistsTab

                val isFromPlaylist = UrlParts.queryParameter(url, DeepLinkConstants.QueryKey.ENTRY) ==
                    DeepLinkConstants.PlayerScreenEntry.Playlist.path

                if (isFromPlaylist && playingPlaylistId != null) {
                    intents += NavigationEvent.NavigateToPlaylistContent(
                        playlistId = playingPlaylistId,
                        focusAudioMediaId = playingMediaId,
                    )
                }
                intents += NavigationEvent.NavigateToPlayer
            }

            DeepLinkConstants.Path.PLAYLISTS -> {
                intents += NavigationEvent.NavigateToMainPlaylistsTab

                val playlistId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationEvent.NavigateToPlaylistContent(
                    playlistId = playlistId,
                    focusAudioMediaId = UrlParts
                        .queryParameter(url, DeepLinkConstants.QueryKey.FOCUS_AUDIO_MEDIA_ID)
                        ?.toIntOrNull(),
                )

                if (pathSegments.getOrNull(3) == DeepLinkConstants.Path.AUDIO_MEDIAS) {
                    pathSegments.getOrNull(4)?.toIntOrNull()?.let { audioMediaId ->
                        intents += NavigationEvent.NavigateToAudioMedia(audioMediaId)
                    }
                }
            }

            DeepLinkConstants.Path.AUDIO_MEDIAS -> {
                val audioMediaId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationEvent.NavigateToMainPlaylistsTab
                intents += NavigationEvent.NavigateToAudioMedia(audioMediaId)
            }

            DeepLinkConstants.Path.LYRICS -> {
                val audioMediaId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationEvent.NavigateToMainPlaylistsTab
                intents += NavigationEvent.NavigateToLyricsViewer(audioMediaId)
            }
        }

        return intents
    }

    /** 링크가 주소를 한 번 인코딩해 실어 오므로(`encodedUrl`) 쿼리에서 꺼낸 뒤 한 번 더 디코딩한다. */
    private fun targetUrlOf(url: String): String? =
        UrlParts.queryParameter(url, DeepLinkConstants.QueryKey.ENCODED_URL)
            ?.let { UrlParts.percentDecode(it, plusAsSpace = true) }
}
