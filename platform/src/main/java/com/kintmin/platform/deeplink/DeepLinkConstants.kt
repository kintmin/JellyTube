package com.kintmin.platform.deeplink

object DeepLinkConstants {

    private const val DEEP_LINK_SCHEME_AND_HOST = "https://www.jellytube.app.com"   // 변경 시 app manifest intent-filter 변경 필요
    private const val DEEP_LINK_MAIN_PATH = "main"
    private const val DEEP_LINK_BASE_PATH = "$DEEP_LINK_SCHEME_AND_HOST/$DEEP_LINK_MAIN_PATH"

    object Key {
        const val MAIN_TAB_TYPE = "mainTabType" // // download, playlist
        const val ENCODED_URL = "encodedUrl"
        const val PLAYLIST_ID = "playlistId"
    }

    object UriPattern {
        const val DOWNLOAD_SCREEN = "$DEEP_LINK_BASE_PATH?tab={${Key.MAIN_TAB_TYPE}}/encodedUrl={${Key.ENCODED_URL}}"
        const val PLAYLIST_SCREEN = "$DEEP_LINK_BASE_PATH?tab={${Key.MAIN_TAB_TYPE}}/playlistId={${Key.PLAYLIST_ID}}"
    }
}