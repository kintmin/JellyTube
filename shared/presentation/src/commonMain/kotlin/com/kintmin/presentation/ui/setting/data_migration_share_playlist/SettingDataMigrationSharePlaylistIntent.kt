package com.kintmin.presentation.ui.setting.data_migration_share_playlist

sealed interface SettingDataMigrationSharePlaylistIntent {

    data class OnClickPlaylistItem(val playlistId: Int) : SettingDataMigrationSharePlaylistIntent

    data object OnClickSelectAll : SettingDataMigrationSharePlaylistIntent

    data object OnClickClearCheckedList : SettingDataMigrationSharePlaylistIntent
}
