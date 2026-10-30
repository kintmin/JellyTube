package com.kintmin.presentation.ui.audio_edit

sealed interface AudioEditEvent {

    data class ShowToast(val message: String) : AudioEditEvent

    data object NavigateToBack : AudioEditEvent
}
