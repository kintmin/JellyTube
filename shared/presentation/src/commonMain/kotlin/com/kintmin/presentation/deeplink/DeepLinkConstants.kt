package com.kintmin.presentation.deeplink

/**
 * 딥링크 경로·쿼리 키·주소 조립의 **단일 진원지**. 주소는 문자열로 다룬다 —
 * `Uri` 가 필요한 자리(`PendingIntent`)는 `android:app` 어댑터가 여기 결과를 바꿔 쓴다.
 */
object DeepLinkConstants {

    const val SCHEME = "https"

    /**
     * iOS 진입용 커스텀 스킴. 호스트·경로 문법은 `https` 쪽과 같다
     * (`jellytube://www.jellytube.com/main/...`).
     *
     * 커스텀 스킴은 다른 앱도 등록할 수 있다. **외부에서 값을 받는 링크가 생기면
     * Universal Links 로 올라가야 한다** — `apple-app-site-association` 호스팅이 선행 조건이다.
     */
    const val APP_SCHEME = "jellytube"

    /** 변경 시 `android:app` manifest 의 intent-filter 와 iOS `CFBundleURLTypes` 도 바꿔야 한다. */
    const val HOST = "www.jellytube.com"

    private const val SCHEME_AND_HOST = "$SCHEME://$HOST"

    object Path {

        const val MAIN = "main"

        const val PLAYER = "player"

        const val SETTINGS = "settings"
        const val APP_LOG = "appLog"
        const val STEP = "step"

        const val DOWNLOAD = "download"

        const val PLAYLISTS = "playlists"
        const val AUDIO_MEDIAS = "audioMedias"

        const val LYRICS = "lyrics"
    }

    object QueryKey {

        const val ENTRY = "entry"
        const val ENCODED_URL = "encodedUrl"
        const val FOCUS_AUDIO_MEDIA_ID = "focusAudioMediaId"
    }

    /** 플레이어 화면에 어디서 들어왔는지. 재생 목록에서 들어오면 목록을 먼저 쌓는다. */
    enum class PlayerScreenEntry(val path: String) {
        Main(Path.MAIN),
        Playlist(Path.PLAYLISTS),
    }

    object UrlBuilder {

        fun playerScreen(entry: PlayerScreenEntry = PlayerScreenEntry.Main): String =
            "$SCHEME_AND_HOST/${Path.MAIN}/${Path.PLAYER}" +
                "?${QueryKey.ENTRY}=${UrlParts.encodeQueryValue(entry.path)}"

        fun settingsScreen(): String = "$SCHEME_AND_HOST/${Path.MAIN}/${Path.SETTINGS}"

        fun appLogScreen(): String = "${settingsScreen()}/${Path.APP_LOG}"

        fun stepScreen(): String = "$SCHEME_AND_HOST/${Path.MAIN}/${Path.STEP}"

        fun mainScreenDownloadTab(downloadLink: String): String =
            "$SCHEME_AND_HOST/${Path.MAIN}/${Path.DOWNLOAD}" +
                "?${QueryKey.ENCODED_URL}=${UrlParts.encodeQueryValue(downloadLink)}"

        fun mainScreenPlaylists(): String = "$SCHEME_AND_HOST/${Path.MAIN}/${Path.PLAYLISTS}"

        fun playlistContentScreen(playlistId: Int, audioMediaId: Int? = null): String {
            val base = "${mainScreenPlaylists()}/$playlistId/${Path.AUDIO_MEDIAS}"
            if (audioMediaId == null) return base
            return "$base?${QueryKey.FOCUS_AUDIO_MEDIA_ID}=$audioMediaId"
        }

        fun playlistAudioMediaDetailScreen(playlistId: Int, audioMediaId: Int): String =
            "${mainScreenPlaylists()}/$playlistId/${Path.AUDIO_MEDIAS}/$audioMediaId"

        fun audioMediaDetailScreen(audioMediaId: Int): String =
            "$SCHEME_AND_HOST/${Path.MAIN}/${Path.AUDIO_MEDIAS}/$audioMediaId"

        fun lyricsViewerScreen(audioMediaId: Int): String =
            "$SCHEME_AND_HOST/${Path.MAIN}/${Path.LYRICS}/$audioMediaId"
    }
}
