package com.kintmin.presentation.ui.playlist_detail.list

sealed interface PlaylistDetailListEvent {
    data class NavigateToAudioDetailScreen(val audioMediaId: Int): PlaylistDetailListEvent
}