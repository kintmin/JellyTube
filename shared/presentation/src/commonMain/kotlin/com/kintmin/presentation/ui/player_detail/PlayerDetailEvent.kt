package com.kintmin.presentation.ui.player_detail

sealed interface PlayerDetailEvent {
    data class NavigateToAudioMediaDetailScreen(val audioMediaId: Int) : PlayerDetailEvent
    data class NavigateToAudioMediaInformationEditScreen(val audioMediaId: Int) : PlayerDetailEvent
    data class NavigateToLyricsSearch(
        val audioMediaId: Int,
        val query: String,
        val durationSeconds: Double,
    ) : PlayerDetailEvent
    data class NavigateToLyricsViewer(val audioMediaId: Int) : PlayerDetailEvent
    data class NavigateToPlayingPlaylist(val playlistId: Int, val audioMediaId: Int?) : PlayerDetailEvent
    data class ShowToast(val message: String) : PlayerDetailEvent
}
