package com.kintmin.presentation.deeplink

import com.kintmin.presentation.navigation.NavigationIntent

/**
 * 앱 밖에서 들어온 주소를 화면 이동 명령 목록으로 바꾼다.
 *
 * **순수 함수다.** 포트를 주입받지 않고 재생 상태를 파라미터로 받는다 — 파서가 포트를 들면
 * 테스트에 목이 필요해지고, 두 플랫폼이 같은 답을 내는지 확인하기 어려워진다.
 *
 * 우리 주소가 아니면 **빈 목록**을 돌려준다. 호출자는 아무것도 하지 않으면 된다.
 */
object DeepLinkParser {

    /**
     * @param playingPlaylistId 재생 중인 플레이리스트. 플레이어 딥링크가 "재생 목록에서
     *   들어왔다" 고 알려 줄 때만 쓴다. 호출자가 미디어 컨트롤러에서 읽어 넘긴다.
     * @param playingMediaId 재생 중인 음원. 위와 같은 자리에서만 쓴다.
     */
    fun parse(
        url: String,
        playingPlaylistId: Int? = null,
        playingMediaId: Int? = null,
    ): List<NavigationIntent> {
        // 스킴만 둘이고 호스트·경로 문법은 하나다. Android 는 https(App Links),
        // iOS 는 커스텀 스킴으로 들어온다.
        val scheme = UrlParts.scheme(url)
        if (scheme != DeepLinkConstants.SCHEME && scheme != DeepLinkConstants.APP_SCHEME) {
            return emptyList()
        }
        if (UrlParts.host(url) != DeepLinkConstants.HOST) return emptyList()

        val pathSegments = UrlParts.pathSegments(url)
        if (pathSegments.getOrNull(0) != DeepLinkConstants.Path.MAIN) return emptyList()

        // 뿌리 경로까지 맞으면 먼저 스택을 비운다. 이후 분기가 중간에 포기해도 이것은 남는다.
        val intents = mutableListOf<NavigationIntent>(NavigationIntent.PopAll)

        when (pathSegments.getOrNull(1)) {
            DeepLinkConstants.Path.SETTINGS -> {
                intents += NavigationIntent.NavigateToMainPlaylistsTab
                intents += NavigationIntent.NavigateToSettings

                if (pathSegments.getOrNull(2) == DeepLinkConstants.Path.APP_LOG) {
                    intents += NavigationIntent.NavigateToSettingAppLog
                }
            }

            DeepLinkConstants.Path.STEP -> {
                intents += NavigationIntent.NavigateToMainPlaylistsTab
                intents += NavigationIntent.NavigateToSettings
                intents += NavigationIntent.NavigateToStep
            }

            DeepLinkConstants.Path.DOWNLOAD -> {
                intents += NavigationIntent.NavigateToMainDownloadTab(targetUrlOf(url))
            }

            DeepLinkConstants.Path.PLAYER -> {
                intents += NavigationIntent.NavigateToMainPlaylistsTab

                val isFromPlaylist = UrlParts.queryParameter(url, DeepLinkConstants.QueryKey.ENTRY) ==
                    DeepLinkConstants.PlayerScreenEntry.Playlist.path

                if (isFromPlaylist && playingPlaylistId != null) {
                    intents += NavigationIntent.NavigateToPlaylistContent(
                        playlistId = playingPlaylistId,
                        focusAudioMediaId = playingMediaId,
                    )
                }
                intents += NavigationIntent.NavigateToPlayer
            }

            DeepLinkConstants.Path.PLAYLISTS -> {
                intents += NavigationIntent.NavigateToMainPlaylistsTab

                val playlistId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationIntent.NavigateToPlaylistContent(
                    playlistId = playlistId,
                    focusAudioMediaId = UrlParts
                        .queryParameter(url, DeepLinkConstants.QueryKey.FOCUS_AUDIO_MEDIA_ID)
                        ?.toIntOrNull(),
                )

                if (pathSegments.getOrNull(3) == DeepLinkConstants.Path.AUDIO_MEDIAS) {
                    pathSegments.getOrNull(4)?.toIntOrNull()?.let { audioMediaId ->
                        intents += NavigationIntent.NavigateToAudioMedia(audioMediaId)
                    }
                }
            }

            DeepLinkConstants.Path.AUDIO_MEDIAS -> {
                val audioMediaId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationIntent.NavigateToMainPlaylistsTab
                intents += NavigationIntent.NavigateToAudioMedia(audioMediaId)
            }

            DeepLinkConstants.Path.LYRICS -> {
                val audioMediaId = pathSegments.getOrNull(2)?.toIntOrNull() ?: return intents
                intents += NavigationIntent.NavigateToMainPlaylistsTab
                intents += NavigationIntent.NavigateToLyricsViewer(audioMediaId)
            }
        }

        return intents
    }

    /**
     * 다운로드 딥링크의 대상 주소.
     *
     * **두 번 디코딩한다.** 링크가 주소를 한 번 인코딩해 쿼리에 실어 오고(키 이름이
     * `encodedUrl` 인 이유다), 쿼리 값을 꺼낼 때 한 번 더 디코딩되기 때문이다. 안드로이드가
     * `Uri.getQueryParameter` + `URLDecoder.decode` 로 하던 횟수와 같다.
     *
     * 값이 없으면 null 이다. 안드로이드 구현은 이 자리에서 `URLDecoder.decode(null)` 로
     * 예외를 던졌는데, 받는 쪽이 이미 nullable 이라 null 로 흘려보내는 쪽이 맞다.
     */
    private fun targetUrlOf(url: String): String? =
        UrlParts.queryParameter(url, DeepLinkConstants.QueryKey.ENCODED_URL)
            ?.let { UrlParts.percentDecode(it, plusAsSpace = true) }
}
