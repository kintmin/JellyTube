package com.kintmin.presentation.ui.setting.data_migration_receive

sealed interface SettingDataMigrationReceiveEvent {
    data object NavigateToBack : SettingDataMigrationReceiveEvent
}
