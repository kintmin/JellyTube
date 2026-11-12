package com.kintmin.presentation.ui.main.floating_action

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class MainFloatingActionUiState(
    val playlistIdOnDownload: Int? = null,
    val playlistIdOnDownloadName: String = PlaylistType.UNCATEGORIZED.defaultName,
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistBottomSheetVisible: Boolean = false,
)
