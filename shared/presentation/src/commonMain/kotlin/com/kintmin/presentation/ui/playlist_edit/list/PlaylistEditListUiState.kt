package com.kintmin.presentation.ui.playlist_edit.list

import com.kintmin.presentation.ui.playlist_edit.header.PlaylistEditHeaderUiState

data class PlaylistEditListUiState(
    val headerData: PlaylistEditHeaderUiState,
    val audioMediaList: List<PlaylistEditListItemUiState> = emptyList(),
    val checkedItemCount: Int = 0,
)
