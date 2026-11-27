package com.kintmin.presentation.ui.setting

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class SettingUiState(
    val shouldInsertAtTopOnDownload: Boolean = false,
    val playlistIdOnDownload: Int? = null,
    val playlistIdOnDownloadName: String = PlaylistType.UNCATEGORIZED.defaultName,
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistIdOnDownloadBottomSheetVisible: Boolean = false,
    val isImportPlaylistBottomSheetVisible: Boolean = false,
    val isImporting: Boolean = false,
    val isStepEnabled: Boolean = true,
    val deviceId: String? = null,
    val backupDescription: String = "",
    val isBackupWarning: Boolean = false,
    val isCheckingAppUpdate: Boolean = false,
)
