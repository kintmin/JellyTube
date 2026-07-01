package com.kintmin.presentation.ui.main.youtube_search

sealed interface YoutubeDownloadIntent {
    data class OnClickDownload(val url: String): YoutubeDownloadIntent
    data class OnChangeUrl(val url: String): YoutubeDownloadIntent
    data class OnShowToast(val message: String): YoutubeDownloadIntent
}