package com.kintmin.domain.backup.usecase

import com.kintmin.domain.audio_media.DownloadSizeEstimate
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.RestorePreview
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.youtube.YoutubeUrl

class PreviewRestoreUseCase(
    private val dataMigrationRepository: DataMigrationRepository,
) {
    suspend operator fun invoke(backup: BackupContent): Result<RestorePreview> = runCatching {
        val targetKeys = dataMigrationRepository.fetchSnapshot().getOrThrow()
            .audioMedias.map { SourceKey.of(it.source) }.toSet()
        val audios = backup.snapshot.audioMedias.distinctBy { SourceKey.of(it.source) }
        val (existing, incoming) = audios.partition { SourceKey.of(it.source) in targetKeys }
        val (redownload, notRestorable) = incoming.partition { YoutubeUrl.videoIdOf(it.source) != null }
        RestorePreview(
            createdAtMillis = backup.createdAtMillis,
            totalCount = audios.size,
            alreadyOnDeviceCount = existing.size,
            redownloadCount = redownload.size,
            notRestorableCount = notRestorable.size,
            estimatedDownloadBytes = redownload.sumOf { DownloadSizeEstimate.bytesFor(it.rawAudioDurationSeconds) },
        )
    }
}
