package com.kintmin.presentation.ui.main.floating_action

sealed interface MainFloatingActionIntent {
    data object OnClickPlaylistButton : MainFloatingActionIntent
    data object OnDismissPlaylistBottomSheet : MainFloatingActionIntent
    data class OnSelectPlaylist(val playlistId: Int) : MainFloatingActionIntent
}
