package com.kintmin.presentation.ui.audio_media_edit

sealed interface AudioMediaEditIntent {
    data class OnAudioMediaNameChanged(val text: String): AudioMediaEditIntent
    data class OnAudioMediaArtistChanged(val text: String): AudioMediaEditIntent
    data class OnAudioMediaDescriptionChanged(val text: String): AudioMediaEditIntent
    data class OnClickDeleteLinkedPlaylist(val playlistId: Int): AudioMediaEditIntent
    data object OnClickShowAddPlaylistBottomSheet : AudioMediaEditIntent
    data object OnDismissAddPlaylistBottomSheet : AudioMediaEditIntent
    data class OnClickAddLinkedPlaylist(val playlistId: Int): AudioMediaEditIntent
}