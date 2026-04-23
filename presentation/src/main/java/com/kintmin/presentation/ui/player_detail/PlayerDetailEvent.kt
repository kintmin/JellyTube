package com.kintmin.presentation.ui.player_detail

sealed interface PlayerDetailEvent {
    data class NavigateToAudioMediaDetailScreen(val audioMediaId: Int) : PlayerDetailEvent
    data class NavigateToAudioMediaEditScreen(val audioMediaId: Int) : PlayerDetailEvent
    data class NavigateToPlayingPlaylist(val playlistId: Int, val audioMediaId: Int?) : PlayerDetailEvent
    data class ShowToast(val message: String) : PlayerDetailEvent
}
