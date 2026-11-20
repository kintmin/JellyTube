package com.kintmin.presentation.ui.playlist_add

sealed interface PlaylistAddEvent {
    data object NavigateToBack : PlaylistAddEvent

    data class ShowToast(val message: String) : PlaylistAddEvent
}
