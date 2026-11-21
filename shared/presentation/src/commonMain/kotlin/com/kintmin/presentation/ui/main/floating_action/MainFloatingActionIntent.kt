package com.kintmin.presentation.ui.main.floating_action

sealed interface MainFloatingActionIntent {
    data class OnClickDownload(val url: String) : MainFloatingActionIntent
    data object OnClickPlaylistButton : MainFloatingActionIntent
    data object OnDismissPlaylistBottomSheet : MainFloatingActionIntent
    data class OnSelectPlaylist(val playlistId: Int) : MainFloatingActionIntent
    data object OnClickCreatePlaylist : MainFloatingActionIntent
    data object OnDismissCreatePlaylistDialog : MainFloatingActionIntent
    data class OnCreatePlaylist(val title: String) : MainFloatingActionIntent

    /** 재생목록에서 연 영상을 받을 때 그 곡만 받을지 재생목록 전체를 받을지 고른다. */
    data object OnChooseSingleDownload : MainFloatingActionIntent
    data object OnChoosePlaylistDownload : MainFloatingActionIntent
    data object OnDismissDownloadChoice : MainFloatingActionIntent
}
