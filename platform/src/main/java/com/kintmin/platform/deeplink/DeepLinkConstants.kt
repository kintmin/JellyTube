package com.kintmin.platform.deeplink

import android.net.Uri
import androidx.core.net.toUri

object DeepLinkConstants {

    const val DEEP_LINK_SCHEME = "https"
    const val DEEP_LINK_HOST = "www.jellytube.com" // 변경 시 app manifest intent-filter 변경 필요

    private const val DEEP_LINK_SCHEME_AND_HOST = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST"

    object Path {

        const val MAIN = "main"

        const val PLAYER = "player"

        const val SETTINGS = "settings"
        const val APP_LOG = "appLog"

        const val DOWNLOAD = "download"

        const val PLAYLISTS = "playlists"
        const val PLAYLIST_ID = "playlistId"
        const val AUDIO_MEDIAS = "audioMedias"
        const val AUDIO_MEDIA_ID = "audioMediaId"
    }

    object QueryKey {

        const val ENCODED_URL = "encodedUrl"
        const val FOCUS_AUDIO_MEDIA_ID = "focusAudioMediaId"
    }

    object UriPattern {

        const val PLAYER_SCREEN = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.PLAYER}"

        const val SETTINGS_SCREEN = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.SETTINGS}"
        const val APP_LOG_SCREEN = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.SETTINGS}/${Path.APP_LOG}"

        const val MAIN_SCREEN_DOWNLOAD = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.DOWNLOAD}?${QueryKey.ENCODED_URL}={${QueryKey.ENCODED_URL}}"
        const val MAIN_SCREEN_PLAYLISTS = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.PLAYLISTS}"

        const val PLAYLIST_CONTENT_SCREEN = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.PLAYLISTS}/{${Path.PLAYLIST_ID}}/${Path.AUDIO_MEDIAS}?${QueryKey.FOCUS_AUDIO_MEDIA_ID}={${QueryKey.FOCUS_AUDIO_MEDIA_ID}}"
        const val AUDIO_MEDIA_SCREEN = "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}/${Path.PLAYLISTS}/{${Path.PLAYLIST_ID}}/${Path.AUDIO_MEDIAS}/{${Path.AUDIO_MEDIA_ID}}"
    }

    object UriBuilder {

//        fun playerBar(playlistId: Int): Uri =
//            "${DEEP_LINK_SCHEME_AND_HOST}/${Path.MAIN}".toUri()
//                .buildUpon()
//                .appendQueryParameter(QueryKey.PLAYLIST_ID, playlistId.toString())
//                .build()
    }
}
