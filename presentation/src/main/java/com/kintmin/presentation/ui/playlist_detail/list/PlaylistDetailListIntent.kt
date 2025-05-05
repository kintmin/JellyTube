package com.kintmin.presentation.ui.playlist_detail.list

sealed interface PlaylistDetailListIntent {
    data class OnClickAudioItem(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickDeleteAudioMediaFile(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
    data class OnClickShowDetailAudioMedia(val data: PlaylistDetailListItemUiState) : PlaylistDetailListIntent
}