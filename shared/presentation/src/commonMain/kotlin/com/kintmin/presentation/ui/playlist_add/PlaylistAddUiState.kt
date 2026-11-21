package com.kintmin.presentation.ui.playlist_add

import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState

data class PlaylistAddUiState(
    // 첫 방출 전에는 null 이라 "추가할 곡이 없다" 가 잠깐 비치지 않는다.
    val audioList: List<PlaylistAddListItemUiState>? = null,
    val checkedItemCount: Int = 0,
    val playlistName: String = "",
)
