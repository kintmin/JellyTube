package com.kintmin.presentation.ui.setting.data_migration_receive

sealed interface SettingDataMigrationReceiveIntent {
    data object OnClickSearch : SettingDataMigrationReceiveIntent
    data class OnChangeCode(val code: String) : SettingDataMigrationReceiveIntent
    data object OnClickConnect : SettingDataMigrationReceiveIntent
    data object OnClickRetry : SettingDataMigrationReceiveIntent

    /** 나가기 확인 다이얼로그에서 나가기를 골랐다. */
    data object OnConfirmExit : SettingDataMigrationReceiveIntent
}
