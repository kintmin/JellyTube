package com.kintmin.presentation.ui.playlist_detail

import com.kintmin.presentation.ui.playlist_detail.list_item.AudioPlayUiState

sealed interface AudioPlayIntent {
    data class OnClickAudioItem(val data: AudioPlayUiState): AudioPlayIntent
    data class OnClickDeleteAudioMedia(val data: AudioPlayUiState): AudioPlayIntent
    data object OnClickPlayAll: AudioPlayIntent
    data object OnClickPlayShuffle: AudioPlayIntent
    data object OnClickAddAudioMediaInPlaylist: AudioPlayIntent
    data object OnClickRepeatPlaylist: AudioPlayIntent
    data object OnClickEditPlaylist: AudioPlayIntent
    data object OnClickReorderAudioMediaList: AudioPlayIntent
    data object OnClickNavigationBack: AudioPlayIntent
}