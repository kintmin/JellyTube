package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository

/**
 * 보내는 기기의 데이터를 받아 이 기기에 합친다. 중간에 끊겨도 다시 부르면 받아 둔 파일은 건너뛰고
 * 이어 받는다. 이미 합친 데이터는 다시 합쳐도 바뀌지 않는다.
 */
class ImportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(
        endpoint: MigrationEndpoint,
        token: String,
        onProgress: (MigrationProgress) -> Unit,
    ): Result<MigrationSummary> = runCatching {
        // 계획은 받는 쪽 시스템 플레이리스트가 다 있어야 세울 수 있다.
        playlistRepository.ensureSystemPlaylists().getOrThrow()

        val export = dataMigrationRepository.fetchRemoteExport(endpoint, token).getOrThrow()
        val availableEntries = export.files.filter { MigrationFiles.isSafeName(it.file.name) }
        val source = MigrationFiles.keepAvailable(export.snapshot, availableEntries.map { it.file }.toSet())
        val target = dataMigrationRepository.fetchSnapshot().getOrThrow()
        val plan = MigrationPlanner.plan(source, target)

        val entries = availableEntries.filter { it.file in plan.requiredFiles }
        val totalBytes = entries.sumOf { it.size }
        var receivedBytes = 0L
        onProgress(MigrationProgress(receivedBytes, totalBytes))
        entries.forEach { entry ->
            val before = receivedBytes
            dataMigrationRepository.stageFile(endpoint, token, entry) { received ->
                onProgress(MigrationProgress(before + received, totalBytes))
            }.getOrThrow()
            receivedBytes = before + entry.size
            onProgress(MigrationProgress(receivedBytes, totalBytes))
        }

        dataMigrationRepository.applyPlan(plan).getOrThrow()
        dataMigrationRepository.clearStaging().getOrThrow()

        MigrationSummary(
            newAudioMediaCount = plan.newAudioMedias.size,
            newPlaylistCount = plan.newPlaylists.size,
        )
    }
}
