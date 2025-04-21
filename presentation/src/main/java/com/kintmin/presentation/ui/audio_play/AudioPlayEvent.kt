package com.kintmin.presentation.ui.audio_play

sealed interface AudioPlayEvent {
    data class ShowToast(val message: String): AudioPlayEvent
}