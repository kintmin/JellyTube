package com.kintmin.presentation.ui.main.playlist

data class PlaylistUiState(
    val playlists: List<PlaylistItemUiState> = emptyList(),
    val isLoaded: Boolean = false,
    val isReorderBottomSheetVisible: Boolean = false,
)
