package com.kintmin.presentation.ui.main.floating_action

import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class MainFloatingActionUiState(
    val playlistIdOnDownload: Int? = null,
    val playlistIdOnDownloadName: String = PlaylistType.UNCATEGORIZED.defaultName,
    val selectablePlaylistList: List<DownloadPlaylistUiState> = emptyList(),
    val isPlaylistBottomSheetVisible: Boolean = false,
    val isCreatePlaylistDialogVisible: Boolean = false,
    /** 재생목록에서 연 영상의 주소. 있으면 무엇을 받을지 묻는다. */
    val downloadChoiceUrl: String? = null,
)
