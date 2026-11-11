package com.kintmin.presentation.ui.setting.data_migration_send

data class SettingDataMigrationSendUiState(
    val step: DataMigrationSendStep = DataMigrationSendStep.Starting,
) {
    /** 받는 기기가 받는 중이다. 이 화면을 떠나면 받는 쪽이 실패하므로 뒤로가기를 막는다. */
    val isSending: Boolean get() = step == DataMigrationSendStep.Sending

    val keepsScreenOn: Boolean get() = step is DataMigrationSendStep.WaitingForPair || isSending

    companion object {
        fun getMock() = SettingDataMigrationSendUiState(step = DataMigrationSendStep.WaitingForPair("482913"))
    }
}

sealed interface DataMigrationSendStep {
    data object Starting : DataMigrationSendStep
    data class WaitingForPair(val code: String) : DataMigrationSendStep
    data object Sending : DataMigrationSendStep
    data class Finished(val newAudioMediaCount: Int, val failedCount: Int) : DataMigrationSendStep

    /** 코드를 여러 번 틀려 연결을 막았다. */
    data object Locked : DataMigrationSendStep
}
