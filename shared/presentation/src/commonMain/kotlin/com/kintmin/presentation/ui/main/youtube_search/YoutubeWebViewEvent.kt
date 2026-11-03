package com.kintmin.presentation.ui.main.youtube_search

sealed interface YoutubeWebViewEvent {
    data class ShowToast(val message: String): YoutubeWebViewEvent
}