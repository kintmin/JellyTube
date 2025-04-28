package com.kintmin.presentation.ui.playlist_detail

import com.kintmin.platform.model.AudioPlayData

sealed interface AudioPlayEvent {
    data object NavigateToBack: AudioPlayEvent
    data class ShowToast(val message: String) : AudioPlayEvent
    data class RegisterPlaylist(
        val audioMediaList: ArrayList<AudioPlayData>,
        val startIndex: Int,
        val shouldClear: Boolean,
    ) : AudioPlayEvent
}