package com.kintmin.presentation.ui.setting

import com.kintmin.domain.playlist.model.Playlist

data class SettingUiState(
    val shouldInsertAtTopOnDownload: Boolean = false,
    val playlistIdOnDownload: Int = Playlist.UNCATEGORIZED,
    val playlistIdOnDownloadName: String = "기본",
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistIdOnDownloadBottomSheetVisible: Boolean = false,
    val isStepEnabled: Boolean = true,
)

data class DownloadPlaylistUiState(
    val id: Int,
    val name: String,
    val isSelected: Boolean,
)
