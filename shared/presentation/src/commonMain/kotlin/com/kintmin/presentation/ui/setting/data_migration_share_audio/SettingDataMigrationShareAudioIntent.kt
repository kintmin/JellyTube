package com.kintmin.presentation.ui.setting.data_migration_share_audio

sealed interface SettingDataMigrationShareAudioIntent {

    data class OnChangeSearchText(val searchText: String) : SettingDataMigrationShareAudioIntent

    data class OnClickAudioItem(val audioMediaId: Int) : SettingDataMigrationShareAudioIntent

    data object OnClickSelectAll : SettingDataMigrationShareAudioIntent

    data object OnClickClearCheckedList : SettingDataMigrationShareAudioIntent
}
