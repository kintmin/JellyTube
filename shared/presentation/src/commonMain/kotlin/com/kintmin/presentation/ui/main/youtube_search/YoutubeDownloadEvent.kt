package com.kintmin.presentation.ui.main.youtube_search

sealed interface YoutubeDownloadEvent {
    data class ShowToast(val message: String): YoutubeDownloadEvent
}