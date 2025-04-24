package com.kintmin.presentation.ui.audio_play

import com.kintmin.platform.model.AudioPlayData

sealed interface AudioPlayEvent {
    data class ShowToast(val message: String) : AudioPlayEvent
    data class RegisterPlaylist(
        val playlist: ArrayList<AudioPlayData>,
        val startIndex: Int,
        val clearFlag: Boolean,
    ) : AudioPlayEvent
}