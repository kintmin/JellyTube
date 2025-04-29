package com.kintmin.presentation.ui.playlist_detail

import com.kintmin.presentation.ui.playlist_detail.list_item.AudioPlayUiState

sealed interface AudioPlayEvent {
    data object NavigateToBack: AudioPlayEvent
    data class ShowToast(val message: String) : AudioPlayEvent
    data class RegisterPlaylist(
        val audioMediaList: List<AudioPlayUiState>,
        val startIndex: Int,
        val shouldClear: Boolean,
    ) : AudioPlayEvent
}