package com.kintmin.presentation.ui.setting.data_migration_send

data class SettingDataMigrationSendUiState(
    val step: DataMigrationSendStep = DataMigrationSendStep.Preparing,
    /** null 이면 코드를 여러 번 틀려 연결이 막혔다. [DataMigrationSendStep.Ready] 에서만 의미가 있다. */
    val pairingCode: String? = null,
) {
    companion object {
        fun getMock() = SettingDataMigrationSendUiState(step = DataMigrationSendStep.Ready, pairingCode = "482913")
    }
}

sealed interface DataMigrationSendStep {
    data object Preparing : DataMigrationSendStep
    data object Ready : DataMigrationSendStep
    data object PreparationFailed : DataMigrationSendStep
}
