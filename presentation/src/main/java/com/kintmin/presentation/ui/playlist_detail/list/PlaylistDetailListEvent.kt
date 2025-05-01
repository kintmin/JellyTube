package com.kintmin.presentation.ui.playlist_detail.list

sealed interface PlaylistDetailListEvent {
    data object NavigateToAudioDetailScreen: PlaylistDetailListEvent
}