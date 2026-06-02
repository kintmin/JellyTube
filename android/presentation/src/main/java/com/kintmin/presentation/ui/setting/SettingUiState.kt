package com.kintmin.presentation.ui.setting

import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class SettingUiState(
    val shouldInsertAtTopOnDownload: Boolean = false,
    val playlistIdOnDownload: Int = Playlist.UNCATEGORIZED,
    val playlistIdOnDownloadName: String = "기본",
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistIdOnDownloadBottomSheetVisible: Boolean = false,
    val isStepEnabled: Boolean = true,
)
