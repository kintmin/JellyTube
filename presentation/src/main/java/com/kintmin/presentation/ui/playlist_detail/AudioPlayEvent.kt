package com.kintmin.presentation.ui.playlist_detail

sealed interface AudioPlayEvent {
    data object NavigateToBack: AudioPlayEvent
    data class ShowToast(val message: String) : AudioPlayEvent
}