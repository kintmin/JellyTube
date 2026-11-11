package com.kintmin.presentation.ui.setting.data_migration_send

sealed interface SettingDataMigrationSendIntent {
    data object OnClickRenewCode : SettingDataMigrationSendIntent
    data object OnClickRetryPreparation : SettingDataMigrationSendIntent
}
