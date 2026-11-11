package com.kintmin.presentation.ui.setting.data_migration_send

import com.kintmin.presentation.port.model.RunningWork

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

    /** 이전 전에 끝나 있어야 하는 작업이 남아 있다. 연결을 열지 않는다. */
    data class Blocked(val runningWorks: Map<RunningWork, Int>) : DataMigrationSendStep
    data class WaitingForPair(val code: String) : DataMigrationSendStep
    data object Sending : DataMigrationSendStep
    data class Finished(val newAudioMediaCount: Int, val failedCount: Int) : DataMigrationSendStep

    /** 코드를 여러 번 틀려 연결을 막았다. */
    data object Locked : DataMigrationSendStep

    /** 화면을 떠나지 않았는데 서버가 내려갔다(시스템 종료 등). 보이던 코드는 더 이상 받지 않는다. */
    data object Disconnected : DataMigrationSendStep
}
