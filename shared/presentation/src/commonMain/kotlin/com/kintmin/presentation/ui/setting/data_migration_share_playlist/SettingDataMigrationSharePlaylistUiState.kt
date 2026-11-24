package com.kintmin.presentation.ui.setting.data_migration_share_playlist

import com.kintmin.presentation.ui.setting.data_migration_share_playlist.list.SettingDataMigrationSharePlaylistListItemUiState

data class SettingDataMigrationSharePlaylistUiState(
    val playlistList: List<SettingDataMigrationSharePlaylistListItemUiState> = emptyList(),
    val checkedPlaylistIdList: List<Int> = emptyList(),
) {
    val checkedCount: Int get() = checkedPlaylistIdList.size
    val isAllChecked: Boolean get() = playlistList.isNotEmpty() && checkedCount == playlistList.size
    val isSendEnabled: Boolean get() = checkedCount > 0

    companion object {
        fun getMock() = SettingDataMigrationSharePlaylistUiState(
            playlistList = SettingDataMigrationSharePlaylistListItemUiState.getMockList(),
            checkedPlaylistIdList = SettingDataMigrationSharePlaylistListItemUiState.getMockList()
                .filter { it.isChecked }.map { it.id },
        )
    }
}
