package com.kintmin.presentation.ui.setting.data_migration_receive

import com.kintmin.presentation.port.model.MigrationSource
import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.common.DownloadPlaylistUiState

data class SettingDataMigrationReceiveUiState(
    val phase: DataMigrationReceivePhase = DataMigrationReceivePhase.Idle,
    /** 코드를 입력할 상대. 받는 동안에도 유지한다. */
    val source: MigrationSource? = null,
    val code: String = "",
    val stages: List<DataMigrationStageItem> = emptyList(),
) {
    val canConnect: Boolean get() = code.length == CODE_LENGTH && phase is DataMigrationReceivePhase.CodeInput

    /** 연결이 살아 있어 화면이 꺼지면 안 되는 동안. 받는 사람이 고르는 확인 화면도 그렇다. */
    val isRunning: Boolean
        get() = phase == DataMigrationReceivePhase.Pairing || phase == DataMigrationReceivePhase.Running ||
            phase is DataMigrationReceivePhase.Confirming

    /** 나가기 전에 물어볼 내용. null 이면 그냥 나간다. */
    val exitConfirmation: DataMigrationExitConfirmation?
        get() = when (val phase = phase) {
            DataMigrationReceivePhase.Pairing, DataMigrationReceivePhase.Running -> DataMigrationExitConfirmation.StopRunning
            is DataMigrationReceivePhase.Confirming -> DataMigrationExitConfirmation.StopConfirming
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
    data class Blocked(val runningTasks: Map<RunningTask, Int>) : DataMigrationReceivePhase
    data object NotFound : DataMigrationReceivePhase

    data class SelectSource(val sources: List<MigrationSource>) : DataMigrationReceivePhase
    data class CodeInput(val errorMessage: String? = null) : DataMigrationReceivePhase
    data object Pairing : DataMigrationReceivePhase

    /** 목록은 받았고 파일은 아직 받지 않았다. 받는 사람이 어떻게 받을지 정한다. */
    data class Confirming(
        val newAudioMediaCount: Int,
        val playlists: List<DataMigrationConfirmPlaylistItem>,
        val secretPlaylistCount: Int,
        /** 곡만 왔을 때 담을 곳 후보. 비어 있으면 고를 것이 없다. */
        val destinationCandidates: List<DownloadPlaylistUiState> = emptyList(),
        /** 비밀 플레이리스트도 받는다. 기본은 받지 않기다 — 켜지 않으면 나머지만 들어온다. */
        val acceptsSecret: Boolean = false,
    ) : DataMigrationReceivePhase {
        val conflictingPlaylists: List<DataMigrationConfirmPlaylistItem> get() = playlists.filter { it.isConflicting }
        val mergingSourceIds: Set<Int> get() = playlists.filter { it.isMerging }.map { it.sourceId }.toSet()
        val destinationPlaylistId: Int? get() = destinationCandidates.firstOrNull { it.isSelected }?.id
    }

    data object Running : DataMigrationReceivePhase

    /** 연결이 끊기는 등으로 멈췄다. 다시 시도하면 받아 둔 파일은 건너뛴다. */
    data class Interrupted(val message: String) : DataMigrationReceivePhase
    data class Finished(
        val newAudioMediaCount: Int,
        val newPlaylistCount: Int,
        val redownloadedCount: Int,
        val failures: List<DataMigrationFailureItem>,
        /** 함께 받은 비밀 플레이리스트 수. 받은 뒤에는 비밀모드를 켜야 보이므로 그것을 알린다. */
        val secretPlaylistCount: Int = 0,
    ) : DataMigrationReceivePhase
}

enum class DataMigrationStageKind(val label: String) {
    CLEANUP("이전 시도 잔여물 정리"),
    PLANNING("목록 받기·계획"),
    REDOWNLOAD("재생 호환성 확인·원본에서 다시 받기"),
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

data class DataMigrationConfirmPlaylistItem(
    /** 보내는 기기의 플레이리스트 id. 합칠 것을 가리킬 때 쓴다. */
    val sourceId: Int,
    val name: String,
    val audioMediaCount: Int,
    val isSecret: Boolean,
    /** 이 기기에 같은 이름이 이미 있다. */
    val isConflicting: Boolean,
    /** 그 플레이리스트에 합친다. 기본은 따로 만들기다. */
    val isMerging: Boolean,
)

data class DataMigrationFailureItem(
    val audioMediaName: String,
    val reason: String,
)

sealed interface DataMigrationExitConfirmation {
    data object StopRunning : DataMigrationExitConfirmation

    /** 확인 화면에서 나간다. 받지 않기와 같아서 보내는 기기에도 알려야 한다. */
    data object StopConfirming : DataMigrationExitConfirmation
    data object StopInterrupted : DataMigrationExitConfirmation
    data class LeaveFailures(val failedCount: Int) : DataMigrationExitConfirmation
}
