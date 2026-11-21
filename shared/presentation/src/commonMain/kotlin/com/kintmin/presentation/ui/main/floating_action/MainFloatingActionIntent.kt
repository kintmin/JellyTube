package com.kintmin.presentation.ui.main.floating_action

sealed interface MainFloatingActionIntent {
    data class OnClickDownload(val url: String) : MainFloatingActionIntent
    data object OnClickPlaylistButton : MainFloatingActionIntent
    data object OnDismissPlaylistBottomSheet : MainFloatingActionIntent
    data class OnSelectPlaylist(val playlistId: Int) : MainFloatingActionIntent
    data object OnClickCreatePlaylist : MainFloatingActionIntent
    data object OnDismissCreatePlaylistDialog : MainFloatingActionIntent
    data class OnCreatePlaylist(val title: String) : MainFloatingActionIntent
}
