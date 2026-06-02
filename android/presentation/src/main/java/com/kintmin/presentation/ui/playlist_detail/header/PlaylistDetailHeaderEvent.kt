package com.kintmin.presentation.ui.playlist_detail.header

sealed interface PlaylistDetailHeaderEvent {
    data object NavigateToAddAudioMediaScreen : PlaylistDetailHeaderEvent
    data object NavigateToEditPlaylistScreen : PlaylistDetailHeaderEvent
}