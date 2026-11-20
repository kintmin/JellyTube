package com.kintmin.presentation.ui.playlist_edit.list

sealed interface PlaylistEditListEvent {
    data class ShowToast(val message: String) : PlaylistEditListEvent
}
