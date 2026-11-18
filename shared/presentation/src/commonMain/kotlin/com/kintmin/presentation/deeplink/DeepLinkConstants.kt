package com.kintmin.presentation.deeplink

object DeepLinkConstants {

    const val SCHEME = "https"

    /** 커스텀 스킴은 다른 앱도 등록할 수 있다. 외부에서 값을 받는 링크에는 쓸 수 없다. */
    const val APP_SCHEME = "jellytube"

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
