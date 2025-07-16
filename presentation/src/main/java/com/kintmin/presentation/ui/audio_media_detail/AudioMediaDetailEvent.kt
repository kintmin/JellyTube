package com.kintmin.presentation.ui.audio_media_detail

sealed interface AudioMediaDetailEvent {
    data object OnNavigateToBack : AudioMediaDetailEvent
}