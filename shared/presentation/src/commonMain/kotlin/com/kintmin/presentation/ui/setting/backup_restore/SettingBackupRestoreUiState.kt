package com.kintmin.presentation.ui.setting.backup_restore

import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus

data class SettingBackupRestoreUiState(
    val folderLabel: String? = null,
    val backupDescription: String = "",
    val isBackupFailing: Boolean = false,
    val isBackingUp: Boolean = false,
    val restore: RestorePhase = RestorePhase.Finding,
    val stages: List<DataMigrationStageItem> = emptyList(),
) {
    val isRestoring: Boolean get() = restore == RestorePhase.Running

    /** 보관함에 반영하기 시작하면 끝까지 간다. 멈추면 반영된 것을 멈췄다고 안내하게 된다. */
    val canStop: Boolean
        get() = isRestoring &&
            stages.none { it.kind == DataMigrationStageKind.APPLY && it.status == DataMigrationStageStatus.IN_PROGRESS }

    /** 나가기 전에 물어볼 내용. null 이면 그냥 나간다. */
    val exitConfirmation: RestoreExitConfirmation?
        get() = if (isRestoring) RestoreExitConfirmation.StopRunning else null

    companion object {
        val RESTORE_STAGES = listOf(DataMigrationStageKind.PLANNING, DataMigrationStageKind.REDOWNLOAD, DataMigrationStageKind.APPLY)

        fun restoreStageLabel(kind: DataMigrationStageKind): String = when (kind) {
            DataMigrationStageKind.PLANNING -> "백업과 이 기기 비교"
            DataMigrationStageKind.REDOWNLOAD -> "원본에서 다시 받기"
            else -> kind.label
        }

        fun getMock() = SettingBackupRestoreUiState(
            folderLabel = "Download/JellyTube",
            backupDescription = "마지막 백업 2026년 11월 20일 21시 4분",
            restore = RestorePhase.Preview(RestorePreviewItem("2026년 11월 20일 21시 4분", 452, 12, 431, 9, 1_650)),
        )

        fun getRunningMock() = getMock().copy(
            restore = RestorePhase.Running,
            stages = listOf(
                DataMigrationStageItem(DataMigrationStageKind.PLANNING, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.REDOWNLOAD, DataMigrationStageStatus.IN_PROGRESS, 120, 431),
                DataMigrationStageItem(DataMigrationStageKind.APPLY, DataMigrationStageStatus.PENDING),
            ),
        )
    }
}

sealed interface RestorePhase {
    data object Finding : RestorePhase
    data object NotFound : RestorePhase
    data class ReadFailed(val message: String) : RestorePhase

    /** 복원 전에 끝나 있어야 하는 작업이 남아 있다. */
    data class Blocked(val runningTasks: Map<RunningTask, Int>) : RestorePhase

    /** 무엇을 받을지 보여 주고 복원을 기다린다. [askCellular] 면 Wi-Fi 가 아니라 계속할지 묻는다. */
    data class Preview(
        val preview: RestorePreviewItem,
        val askCellular: Boolean = false,
    ) : RestorePhase
    data object Running : RestorePhase

    /** 멈췄다. 받아 둔 곡은 남아 다시 시도하면 빠진 곡만 받는다. */
    data class Interrupted(val message: String) : RestorePhase
    data class Finished(
        val newAudioMediaCount: Int,
        val newPlaylistCount: Int,
        val failures: List<DataMigrationFailureItem>,
    ) : RestorePhase
}

data class RestorePreviewItem(
    val createdAt: String,
    val totalCount: Int,
    val alreadyOnDeviceCount: Int,
    val redownloadCount: Int,
    val notRestorableCount: Int,
    val estimatedMegabytes: Long,
)

sealed interface RestoreExitConfirmation {
    data object StopRunning : RestoreExitConfirmation
}
