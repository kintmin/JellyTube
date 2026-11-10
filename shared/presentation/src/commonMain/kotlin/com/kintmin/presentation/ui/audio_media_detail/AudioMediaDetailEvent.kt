package com.kintmin.presentation.ui.audio_media_detail

sealed interface AudioMediaDetailEvent {
    data object OnNavigateToBack : AudioMediaDetailEvent

    data class ShowToast(val message: String) : AudioMediaDetailEvent
}