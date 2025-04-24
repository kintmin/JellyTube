package com.kintmin.presentation.ui.audio_play

import com.kintmin.presentation.ui.audio_play.list_item.AudioPlayUiState

sealed interface AudioPlayIntent {
    data class OnClickAudioItem(val data: AudioPlayUiState): AudioPlayIntent
    data class OnClickDeleteAudioMedia(val data: AudioPlayUiState): AudioPlayIntent
    data object PullToRefreshAudioList: AudioPlayIntent
    data object OnClickPlayAll: AudioPlayIntent

}