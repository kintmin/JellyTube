package com.kintmin.presentation.ui.setting.data_migration_send

data class SettingDataMigrationSendUiState(
    /** null 이면 코드를 여러 번 틀려 연결이 막혔다. */
    val pairingCode: String? = null,
) {
    companion object {
        fun getMock() = SettingDataMigrationSendUiState(pairingCode = "482913")
    }
}
