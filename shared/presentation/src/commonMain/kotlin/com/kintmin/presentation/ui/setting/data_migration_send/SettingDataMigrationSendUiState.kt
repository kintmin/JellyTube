package com.kintmin.presentation.ui.setting.data_migration_send

import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.setting.data_migration_share.MigrationShareKind

data class SettingDataMigrationSendUiState(
    val kind: MigrationShareKind = MigrationShareKind.SYNC,
    val sendingCount: Int = 0,
    val step: DataMigrationSendStep = DataMigrationSendStep.Starting,
) {
    /** 받는 기기가 받는 중이다. 이 화면을 떠나면 받는 쪽이 실패하므로 뒤로가기를 막는다. */
    val isSending: Boolean get() = step is DataMigrationSendStep.Sending

    val keepsScreenOn: Boolean get() = step is DataMigrationSendStep.WaitingForPair || isSending

    val title: String get() = when (kind) {
        MigrationShareKind.SYNC -> "다른 기기와 동기화"
        MigrationShareKind.PLAYLIST -> "플레이리스트 보내기"
        MigrationShareKind.AUDIO_MEDIA -> "곡 보내기"
    }

    val summary: String get() = when (kind) {
        MigrationShareKind.SYNC -> "이 기기의 곡·플레이리스트·즐겨찾기·설정·걸음 기록을 모두 보냅니다."
        MigrationShareKind.PLAYLIST -> "고른 플레이리스트 ${sendingCount}개와 그 안의 곡을 보냅니다."
        MigrationShareKind.AUDIO_MEDIA -> "고른 ${sendingCount}곡을 보냅니다."
    }

    companion object {
        fun getMock() = SettingDataMigrationSendUiState(step = DataMigrationSendStep.WaitingForPair("482913"))
    }
}

sealed interface DataMigrationSendStep {
    data object Starting : DataMigrationSendStep

    /** 이전 전에 끝나 있어야 하는 작업이 남아 있다. 연결을 열지 않는다. */
    data class Blocked(val runningTasks: Map<RunningTask, Int>) : DataMigrationSendStep
    data class WaitingForPair(val code: String) : DataMigrationSendStep
    data class Sending(val peerName: String) : DataMigrationSendStep
    data class Finished(val newAudioMediaCount: Int, val failedCount: Int) : DataMigrationSendStep

    /** 코드를 여러 번 틀려 연결을 막았다. */
    data object Locked : DataMigrationSendStep

    /** 화면을 떠나지 않았는데 서버가 내려갔다(시스템 종료 등). 보이던 코드는 더 이상 받지 않는다. */
    data object Disconnected : DataMigrationSendStep
}
