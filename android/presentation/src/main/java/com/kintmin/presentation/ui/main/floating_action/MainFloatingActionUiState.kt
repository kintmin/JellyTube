package com.kintmin.presentation.ui.main.floating_action

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class MainFloatingActionUiState(
    val playlistIdOnDownload: Int = Playlist.UNCATEGORIZED,
    val playlistIdOnDownloadName: String = "기본",
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistBottomSheetVisible: Boolean = false,
)
