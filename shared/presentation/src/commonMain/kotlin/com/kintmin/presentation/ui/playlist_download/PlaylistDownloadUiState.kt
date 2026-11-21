package com.kintmin.presentation.ui.playlist_download

import com.kintmin.presentation.port.model.RunningTask
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationFailureItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageItem
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageKind
import com.kintmin.presentation.ui.setting.data_migration_receive.DataMigrationStageStatus

data class PlaylistDownloadUiState(
    val phase: PlaylistDownloadPhase = PlaylistDownloadPhase.Reading,
    val stages: List<DataMigrationStageItem> = emptyList(),
    /** 받던 곡은 도중에 끊을 수 없어 그 곡이 끝날 때까지 멈추는 중이다. */
    val isStopping: Boolean = false,
) {
    val isRunning: Boolean get() = phase == PlaylistDownloadPhase.Running

    /** 재생목록에 담기 시작하면 끝까지 간다. */
    val canStop: Boolean
        get() = isRunning && !isStopping &&
            stages.none { it.kind == DataMigrationStageKind.APPLY && it.status == DataMigrationStageStatus.IN_PROGRESS }

    companion object {
        val STAGES = listOf(DataMigrationStageKind.REDOWNLOAD, DataMigrationStageKind.APPLY)

        fun stageLabel(kind: DataMigrationStageKind): String = when (kind) {
            DataMigrationStageKind.APPLY -> "재생목록에 담기"
            else -> "곡 받기"
        }

        fun getMock() = PlaylistDownloadUiState(
            phase = PlaylistDownloadPhase.Preview(PlaylistDownloadPreviewItem("드라이브할 때", 48, 5, 41, 2, 160, false)),
        )

        fun getRunningMock() = PlaylistDownloadUiState(
            phase = PlaylistDownloadPhase.Running,
            stages = listOf(
                DataMigrationStageItem(DataMigrationStageKind.REDOWNLOAD, DataMigrationStageStatus.IN_PROGRESS, 17, 41),
                DataMigrationStageItem(DataMigrationStageKind.APPLY, DataMigrationStageStatus.PENDING),
            ),
        )
    }
}

sealed interface PlaylistDownloadPhase {
    data object Reading : PlaylistDownloadPhase
    data class ReadFailed(val message: String) : PlaylistDownloadPhase

    /** 받기 전에 끝나 있어야 하는 작업이 남아 있다. */
    data class Blocked(val runningTasks: Map<RunningTask, Int>) : PlaylistDownloadPhase

    /** [askCellular] 면 Wi-Fi 가 아니라 계속할지 묻는다. */
    data class Preview(
        val preview: PlaylistDownloadPreviewItem,
        val askCellular: Boolean = false,
    ) : PlaylistDownloadPhase
    data object Running : PlaylistDownloadPhase

    /** 멈췄다. 받은 곡은 재생목록에 담겨 있어 다시 받으면 빠진 곡만 받는다. */
    data class Interrupted(val message: String) : PlaylistDownloadPhase
    data class Finished(
        val playlistId: Int?,
        val playlistName: String,
        val downloadedCount: Int,
        val alreadyOnDeviceCount: Int,
        val failures: List<DataMigrationFailureItem>,
        /** 비공개·삭제된 곡은 다시 받아도 받을 수 없어 세지 않는다. */
        val hasRetryableFailures: Boolean,
    ) : PlaylistDownloadPhase
}

data class PlaylistDownloadPreviewItem(
    val title: String,
    val totalCount: Int,
    val alreadyOnDeviceCount: Int,
    val downloadCount: Int,
    val unavailableCount: Int,
    val estimatedMegabytes: Long,
    val mergesIntoExistingPlaylist: Boolean,
) {
    val isLarge: Boolean get() = downloadCount > LARGE_DOWNLOAD_COUNT

    val hasAnythingToAdd: Boolean get() = downloadCount + alreadyOnDeviceCount > 0

    private companion object {
        const val LARGE_DOWNLOAD_COUNT = 200
    }
}
