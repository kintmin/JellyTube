package com.kintmin.presentation.ui.setting.data_migration_receive

import com.kintmin.presentation.port.model.MigrationSource
import com.kintmin.presentation.port.model.RunningWork

data class SettingDataMigrationReceiveUiState(
    val phase: DataMigrationReceivePhase = DataMigrationReceivePhase.Idle,
    /** 코드를 입력할 상대. 받는 동안에도 유지한다. */
    val source: MigrationSource? = null,
    val code: String = "",
    val stages: List<DataMigrationStageItem> = emptyList(),
) {
    val canConnect: Boolean get() = code.length == CODE_LENGTH && phase is DataMigrationReceivePhase.CodeInput

    val isRunning: Boolean
        get() = phase == DataMigrationReceivePhase.Pairing || phase == DataMigrationReceivePhase.Running

    /** 나가기 전에 물어볼 내용. null 이면 그냥 나간다. */
    val exitConfirmation: DataMigrationExitConfirmation?
        get() = when (val phase = phase) {
            DataMigrationReceivePhase.Pairing, DataMigrationReceivePhase.Running -> DataMigrationExitConfirmation.StopRunning
            is DataMigrationReceivePhase.Interrupted -> DataMigrationExitConfirmation.StopInterrupted
            is DataMigrationReceivePhase.Finished ->
                phase.failures.size.takeIf { it > 0 }?.let { DataMigrationExitConfirmation.LeaveFailures(it) }
            else -> null
        }

    companion object {
        const val CODE_LENGTH = 6

        fun getMock() = SettingDataMigrationReceiveUiState(
            phase = DataMigrationReceivePhase.Running,
            stages = listOf(
                DataMigrationStageItem(DataMigrationStageKind.CLEANUP, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.PLANNING, DataMigrationStageStatus.DONE),
                DataMigrationStageItem(DataMigrationStageKind.REDOWNLOAD, DataMigrationStageStatus.DONE, 3, 3),
                DataMigrationStageItem(
                    DataMigrationStageKind.TRANSFER, DataMigrationStageStatus.IN_PROGRESS, 123, 999, 120_000_000, 480_000_000,
                ),
                DataMigrationStageItem(DataMigrationStageKind.APPLY, DataMigrationStageStatus.PENDING),
            ),
        )
    }
}

sealed interface DataMigrationReceivePhase {
    data object Idle : DataMigrationReceivePhase
    data object Searching : DataMigrationReceivePhase

    /** 이전 전에 끝나 있어야 하는 작업이 남아 있다. */
    data class Blocked(val runningWorks: Map<RunningWork, Int>) : DataMigrationReceivePhase
    data object NotFound : DataMigrationReceivePhase

    /** 여러 대를 찾았거나, 찾은 한 대가 버전이 달라 고를 수 없다. */
    data class SelectSource(val sources: List<MigrationSource>) : DataMigrationReceivePhase
    data class CodeInput(val errorMessage: String? = null) : DataMigrationReceivePhase
    data object Pairing : DataMigrationReceivePhase
    data object Running : DataMigrationReceivePhase

    /** 연결이 끊기는 등으로 멈췄다. 다시 시도하면 받아 둔 파일은 건너뛴다. */
    data class Interrupted(val message: String) : DataMigrationReceivePhase
    data class Finished(
        val newAudioMediaCount: Int,
        val newPlaylistCount: Int,
        val redownloadedCount: Int,
        val failures: List<DataMigrationFailureItem>,
    ) : DataMigrationReceivePhase
}

enum class DataMigrationStageKind(val label: String) {
    CLEANUP("이전 시도 잔여물 정리"),
    PLANNING("목록 받기·계획"),
    REDOWNLOAD("iPhone 호환성 확인·원본에서 다시 받기"),
    TRANSFER("파일 받기"),
    APPLY("보관함에 반영"),
}

enum class DataMigrationStageStatus { PENDING, IN_PROGRESS, DONE, FAILED }

data class DataMigrationStageItem(
    val kind: DataMigrationStageKind,
    val status: DataMigrationStageStatus,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val receivedBytes: Long = 0,
    val totalBytes: Long = 0,
)

data class DataMigrationFailureItem(
    val audioMediaName: String,
    val reason: String,
)

sealed interface DataMigrationExitConfirmation {
    data object StopRunning : DataMigrationExitConfirmation
    data object StopInterrupted : DataMigrationExitConfirmation
    data class LeaveFailures(val failedCount: Int) : DataMigrationExitConfirmation
}
