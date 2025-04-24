package com.kintmin.presentation.ui.youtube_search

sealed interface YoutubeDownloadIntent {
    data class OnClickDownload(val url: String): YoutubeDownloadIntent
}