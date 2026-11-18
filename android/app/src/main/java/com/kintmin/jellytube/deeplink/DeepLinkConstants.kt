package com.kintmin.jellytube.deeplink

import android.net.Uri
import androidx.core.net.toUri
import com.kintmin.presentation.deeplink.DeepLinkConstants.PlayerScreenEntry
import com.kintmin.presentation.deeplink.DeepLinkConstants as SharedDeepLink

/** `PendingIntent` 가 `Uri` 를 요구해서 있는 어댑터. */
object DeepLinkConstants {

    const val DEEP_LINK_SCHEME = SharedDeepLink.SCHEME

    const val DEEP_LINK_HOST = SharedDeepLink.HOST

    object UriBuilder {

        fun playerScreen(entry: PlayerScreenEntry = PlayerScreenEntry.Main): Uri =
            SharedDeepLink.UrlBuilder.playerScreen(entry).toUri()

        fun mainScreenDownloadTab(downloadLink: String): Uri =
            SharedDeepLink.UrlBuilder.mainScreenDownloadTab(downloadLink).toUri()

        fun stepScreen(): Uri = SharedDeepLink.UrlBuilder.stepScreen().toUri()

        fun playlistAudioMediaDetailScreen(playlistId: Int, audioMediaId: Int): Uri =
            SharedDeepLink.UrlBuilder.playlistAudioMediaDetailScreen(playlistId, audioMediaId).toUri()
    }
}
