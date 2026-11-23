package com.kintmin.presentation.ui.main.floating_action

sealed interface MainFloatingActionEvent {
    data class Download(val url: String, val playlistId: Int) : MainFloatingActionEvent
    data class NavigateToPlaylistDownload(val url: String) : MainFloatingActionEvent
    data class ShowToast(val message: String) : MainFloatingActionEvent
}
