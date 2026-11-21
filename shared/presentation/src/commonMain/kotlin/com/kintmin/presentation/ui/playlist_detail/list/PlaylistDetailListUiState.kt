package com.kintmin.presentation.ui.playlist_detail.list

data class PlaylistDetailListUiState(
    val audioList: List<PlaylistDetailListItemUiState> = emptyList(),
    // 첫 방출 전의 빈 목록을 "담긴 곡 없음" 으로 그리지 않게 가른다.
    val isLoaded: Boolean = false,
)
