package com.kintmin.presentation.ui.playlist_add

import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState

data class PlaylistAddUiState(
    // 첫 방출 전에는 null 이라 "추가할 곡이 없다" 가 잠깐 비치지 않는다.
    val audioList: List<PlaylistAddListItemUiState>? = null,
    val checkedItemCount: Int = 0,
    val playlistName: String = "",
    /** 담기가 다른 플레이리스트의 연결을 끊으므로 누르기 전에 확인받는다. */
    val isSecretTarget: Boolean = false,
)
