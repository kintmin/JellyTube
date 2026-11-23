package com.kintmin.presentation.ui.setting.data_migration_receive

import com.kintmin.presentation.port.model.MigrationSource

sealed interface SettingDataMigrationReceiveIntent {
    data object OnClickSearch : SettingDataMigrationReceiveIntent
    data class OnSelectSource(val source: MigrationSource) : SettingDataMigrationReceiveIntent
    data class OnChangeCode(val code: String) : SettingDataMigrationReceiveIntent
    data object OnClickConnect : SettingDataMigrationReceiveIntent
    data object OnClickRetry : SettingDataMigrationReceiveIntent

    data class OnToggleMergePlaylist(val sourceId: Int) : SettingDataMigrationReceiveIntent
    data class OnClickMergeAll(val isMerging: Boolean) : SettingDataMigrationReceiveIntent
    data class OnSelectDestinationPlaylist(val playlistId: Int) : SettingDataMigrationReceiveIntent
    data object OnClickStartReceive : SettingDataMigrationReceiveIntent

    /** 확인 화면에서 받지 않기로 했다. */
    data object OnDeclineReceive : SettingDataMigrationReceiveIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : SettingDataMigrationReceiveIntent
}
