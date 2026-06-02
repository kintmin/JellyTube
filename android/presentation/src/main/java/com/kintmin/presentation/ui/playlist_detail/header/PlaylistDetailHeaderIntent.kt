package com.kintmin.presentation.ui.playlist_detail.header

sealed interface PlaylistDetailHeaderIntent {
    data object OnClickShuffle: PlaylistDetailHeaderIntent
    data object OnClickRepeat: PlaylistDetailHeaderIntent
    data object OnClickAdd: PlaylistDetailHeaderIntent
    data object OnClickEdit: PlaylistDetailHeaderIntent
    data object OnClickPlay: PlaylistDetailHeaderIntent
}