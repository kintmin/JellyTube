package com.kintmin.presentation.ui.audio_media_information_edit

sealed interface AudioMediaInformationEditIntent {
    data class OnAudioMediaNameChanged(val text: String): AudioMediaInformationEditIntent
    data class OnAudioMediaArtistChanged(val text: String): AudioMediaInformationEditIntent
    data class OnAudioMediaDescriptionChanged(val text: String): AudioMediaInformationEditIntent
    data class OnClickDeleteLinkedPlaylist(val playlistId: Int): AudioMediaInformationEditIntent
    data object OnClickShowAddPlaylistBottomSheet : AudioMediaInformationEditIntent
    data object OnDismissAddPlaylistBottomSheet : AudioMediaInformationEditIntent
    data class OnClickAddLinkedPlaylist(val playlistId: Int): AudioMediaInformationEditIntent
    // ByteArray 는 내용으로 비교되지 않으므로 data class 로 두지 않는다.
    class OnPickAudioMediaImage(val imageData: ByteArray): AudioMediaInformationEditIntent
    data object OnClickResetAudioMediaImage : AudioMediaInformationEditIntent
}