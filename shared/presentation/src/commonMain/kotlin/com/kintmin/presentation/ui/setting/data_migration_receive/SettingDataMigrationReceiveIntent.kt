package com.kintmin.presentation.ui.setting.data_migration_receive

sealed interface SettingDataMigrationReceiveIntent {
    data object OnClickSearch : SettingDataMigrationReceiveIntent
    data class OnChangeCode(val code: String) : SettingDataMigrationReceiveIntent
    data object OnClickConnect : SettingDataMigrationReceiveIntent
    data object OnClickRetry : SettingDataMigrationReceiveIntent
}
