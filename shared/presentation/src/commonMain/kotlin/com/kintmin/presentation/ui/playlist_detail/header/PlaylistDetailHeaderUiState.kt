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
    // 즐겨찾기는 `isBasePlaylist` 지만 곡을 뺄 수 있다. 둘을 같은 값으로 묶으면 즐겨찾기에서
    // 빼는 길이 막히고 파일 삭제만 남는다.
    val canRemoveTrack: Boolean = false,
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
                canRemoveTrack = true,
            )
        }
    }
}
