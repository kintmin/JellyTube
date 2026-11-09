package com.kintmin.platform.deeplink

import android.net.Uri
import androidx.core.net.toUri
import com.kintmin.presentation.deeplink.DeepLinkConstants.PlayerScreenEntry
import com.kintmin.presentation.deeplink.DeepLinkConstants as SharedDeepLink

/**
 * 공통 딥링크 규약을 안드로이드 `Uri` 로 바꿔 주는 어댑터.
 *
 * 경로·쿼리 키·주소 조립의 단일 진원지는 `shared:presentation` 의
 * [SharedDeepLink] 다. 여기는 `PendingIntent` 가 `Uri` 를 요구해서 존재한다 —
 * **주소 문자열을 여기서 다시 만들지 않는다.**
 */
object DeepLinkConstants {

    const val DEEP_LINK_SCHEME = SharedDeepLink.SCHEME

    /** 변경 시 app manifest 의 intent-filter 도 바꿔야 한다. */
    const val DEEP_LINK_HOST = SharedDeepLink.HOST

    object UriBuilder {

        fun playerScreen(entry: PlayerScreenEntry = PlayerScreenEntry.Main): Uri =
            SharedDeepLink.UrlBuilder.playerScreen(entry).toUri()

        fun mainScreenDownloadTab(downloadLink: String): Uri =
            SharedDeepLink.UrlBuilder.mainScreenDownloadTab(downloadLink).toUri()

        fun stepScreen(): Uri = SharedDeepLink.UrlBuilder.stepScreen().toUri()

        fun appLogScreen(): Uri = SharedDeepLink.UrlBuilder.appLogScreen().toUri()

        fun playlistContentScreen(playlistId: Int, audioMediaId: Int? = null): Uri =
            SharedDeepLink.UrlBuilder.playlistContentScreen(playlistId, audioMediaId).toUri()

        fun playlistAudioMediaDetailScreen(playlistId: Int, audioMediaId: Int): Uri =
            SharedDeepLink.UrlBuilder.playlistAudioMediaDetailScreen(playlistId, audioMediaId).toUri()

        fun audioMediaDetailScreen(audioMediaId: Int): Uri =
            SharedDeepLink.UrlBuilder.audioMediaDetailScreen(audioMediaId).toUri()

        fun lyricsViewerScreen(audioMediaId: Int): Uri =
            SharedDeepLink.UrlBuilder.lyricsViewerScreen(audioMediaId).toUri()
    }
}
