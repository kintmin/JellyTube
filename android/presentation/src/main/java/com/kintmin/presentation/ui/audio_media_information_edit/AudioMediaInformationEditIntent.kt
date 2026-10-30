package com.kintmin.presentation.ui.audio_media_information_edit

sealed interface AudioMediaInformationEditIntent {
    data class OnAudioMediaNameChanged(val text: String): AudioMediaInformationEditIntent
    data class OnAudioMediaArtistChanged(val text: String): AudioMediaInformationEditIntent
    data class OnAudioMediaDescriptionChanged(val text: String): AudioMediaInformationEditIntent
    data class OnClickDeleteLinkedPlaylist(val playlistId: Int): AudioMediaInformationEditIntent
    data object OnClickShowAddPlaylistBottomSheet : AudioMediaInformationEditIntent
    data object OnDismissAddPlaylistBottomSheet : AudioMediaInformationEditIntent
    data class OnClickAddLinkedPlaylist(val playlistId: Int): AudioMediaInformationEditIntent
}