package com.kintmin.presentation.ui.audio_media_information_edit

sealed interface AudioMediaInformationEditEvent {
    data class ShowToast(val message: String) : AudioMediaInformationEditEvent
}
