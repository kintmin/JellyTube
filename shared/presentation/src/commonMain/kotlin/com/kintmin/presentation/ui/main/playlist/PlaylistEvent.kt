package com.kintmin.presentation.ui.main.playlist

sealed interface PlaylistEvent {
    data class NavigateToPlaylistDetailScreen(val playlistInfo: PlaylistItemUiState) : PlaylistEvent
    data class NavigateToPlaylistEditScreen(val playlistId: Int) : PlaylistEvent
    data class NavigateToPlaylistAddScreen(val playlistId: Int, val playlistName: String) : PlaylistEvent
    data class ShowToast(val message: String) : PlaylistEvent

    companion object {
        const val MAKE_FAILED_MESSAGE = "플레이리스트를 만들지 못했습니다. 저장 공간을 확인하고 다시 시도해 주세요."
        const val DELETE_FAILED_MESSAGE = "플레이리스트를 지우지 못했습니다. 잠시 뒤 다시 시도해 주세요."
        const val SET_DOWNLOAD_TARGET_FAILED_MESSAGE = "기본 다운로드 플레이리스트를 바꾸지 못했습니다. 잠시 뒤 다시 시도해 주세요."
        const val REORDER_FAILED_MESSAGE = "순서를 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요."
    }
}