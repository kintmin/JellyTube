package com.kintmin.presentation.ui.audio_media_detail

sealed interface AudioMediaDetailIntent {
    data object OnClickDeleteAudioMedia: AudioMediaDetailIntent
}