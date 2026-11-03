package com.kintmin.presentation.ui.playlist_detail.header

data class PlaylistDetailHeaderUiState(
    val id: Int,
    val imageFileFullPath: String?,
    val name: String,
    val description: String,
    val playlistSubtitle: String,
    val isRepeating: Boolean,
    val isShuffling: Boolean,
    val isBasePlaylist: Boolean = false,
) {
    companion object {
        fun getMock(id: Int = 0): PlaylistDetailHeaderUiState {
            return PlaylistDetailHeaderUiState(
                id = id,
                imageFileFullPath = null,
                name = "새로운 플레이리스트",
                playlistSubtitle = "플레이리스트 · 음원수 123 · 재생시간 999:99:99",
                description = "설명은 최대 100자 설명은 최대 100자 설명은 최대 100자 설명은 최대 100자 ",
                isRepeating = true,
                isShuffling = false,
            )
        }
    }
}
