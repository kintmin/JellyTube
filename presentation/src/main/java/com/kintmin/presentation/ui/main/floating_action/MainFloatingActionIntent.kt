package com.kintmin.presentation.ui.main.floating_action

sealed interface MainFloatingActionIntent {
    data class OnClickDownload(val url: String) : MainFloatingActionIntent
    data object OnClickPlaylistButton : MainFloatingActionIntent
    data object OnDismissPlaylistBottomSheet : MainFloatingActionIntent
    data class OnSelectPlaylist(val playlistId: Int) : MainFloatingActionIntent
}

sealed interface MainFloatingActionEvent {
    data class Download(val url: String) : MainFloatingActionEvent
    data class ShowToast(val message: String) : MainFloatingActionEvent
}
