package com.kintmin.presentation.ui.playlist_add

import com.kintmin.presentation.ui.playlist_add.list.PlaylistAddListItemUiState

sealed interface PlaylistAddIntent {
    data class OnClickAudioItem(val data: PlaylistAddListItemUiState) : PlaylistAddIntent
    data class OnDragSelectItems(val checkedIds: Set<Int>) : PlaylistAddIntent
    data class OnChangeSearchText(val searchText: String) : PlaylistAddIntent
    data object OnClickAdd : PlaylistAddIntent
    data object OnClickClearCheckedItemList : PlaylistAddIntent
}