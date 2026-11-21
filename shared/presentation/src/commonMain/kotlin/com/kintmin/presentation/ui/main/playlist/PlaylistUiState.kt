package com.kintmin.presentation.ui.main.playlist

data class PlaylistUiState(
    val playlists: List<PlaylistItemUiState> = emptyList(),
    val isReorderBottomSheetVisible: Boolean = false,
)
