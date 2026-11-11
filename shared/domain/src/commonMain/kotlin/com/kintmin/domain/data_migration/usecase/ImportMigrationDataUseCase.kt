package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.youtube.YoutubeUrl

/**
 * 보내는 기기의 데이터를 받아 이 기기에 합친다. 중간에 끊겨도 다시 부르면 받아 둔 파일은 건너뛰고
 * 이어 받는다. 이미 합친 데이터는 다시 합쳐도 바뀌지 않는다.
 *
 * 새로 들어올 곡 중 이 기기에서 재생할 수 없는 형식(iOS 의 webm 등)은 파일을 옮기지 않는다.
 * 유튜브 곡은 원본 주소에서 이 기기가 다시 받고, 그 밖의 곡은 들여오지 않고 이름을 알린다.
 */
class ImportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
    private val playlistRepository: PlaylistRepository,
    private val appSettingRepository: AppSettingRepository,
    private val audioMediaRepository: AudioMediaRepository,
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
        val available = MigrationFiles.keepAvailable(export.snapshot, availableEntries.map { it.file }.toSet())
        val target = dataMigrationRepository.fetchSnapshot().getOrThrow()

        val unplayable = MigrationPlanner.plan(available, target).newAudioMedias
            .filterNot { dataMigrationRepository.isPlayableAudioFile(it.audioFileName) }
        val redownloaded = redownload(unplayable.filter { YoutubeUrl.videoIdOf(it.source) != null }, onProgress)
        val excluded = unplayable.filter { it.id !in redownloaded }
        // 같은 곡의 다른 행이 대표로 올라와 재생할 수 없는 파일을 가져오지 않도록 키로 뺀다.
        val excludedKeys = excluded.map { SourceKey.of(it.source) }.toSet()
        val source = available.copy(
            audioMedias = available.audioMedias
                .filter { SourceKey.of(it.source) !in excludedKeys }
                .map { audio -> redownloaded[audio.id]?.let { audio.withAudioFile(it) } ?: audio },
        )
        val redownloadedFiles = redownloaded.values.map { MigrationFile(MigrationFileKind.AUDIO, it.fileNameWithExt) }
        val plan = MigrationPlanner.plan(source, target)
            .let { it.copy(requiredFiles = it.requiredFiles - redownloadedFiles.toSet()) }

        try {
            stageAndApply(endpoint, token, export, availableEntries, plan, target, onProgress)
        } catch (e: Throwable) {
            // 다시 받은 파일은 아직 어느 행도 가리키지 않는다. 두면 고아가 된다.
            redownloaded.values.forEach { audioMediaRepository.deleteFileAtFullPath(it.fileFullPath) }
            throw e
        }

        MigrationSummary(
            newAudioMediaCount = plan.newAudioMedias.size,
            newPlaylistCount = plan.newPlaylists.size,
            redownloadedCount = redownloaded.size,
            excludedAudioMediaNames = excluded.map { it.name },
        )
    }

    private suspend fun stageAndApply(
        endpoint: MigrationEndpoint,
        token: String,
        export: MigrationExport,
        availableEntries: List<MigrationFileEntry>,
        plan: MigrationPlan,
        target: MigrationSnapshot,
        onProgress: (MigrationProgress) -> Unit,
    ) {
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

        val applied = dataMigrationRepository.applyPlan(plan).getOrThrow()
        dataMigrationRepository.clearStaging().getOrThrow()
        export.downloadSettings?.let { applyDownloadSettings(it, plan, applied, target) }
    }

    /** 보내는 쪽 곡 id → 다시 받은 파일. 실패한 곡은 빠진다(들여오지 않는다). */
    private suspend fun redownload(
        audioMedias: List<MigrationAudioMedia>,
        onProgress: (MigrationProgress) -> Unit,
    ): Map<Int, DownloadedAudioFile> {
        val result = mutableMapOf<Int, DownloadedAudioFile>()
        audioMedias.forEachIndexed { index, audio ->
            onProgress(MigrationProgress(0, 0, redownloadedCount = index, redownloadTotal = audioMedias.size))
            audioMediaRepository.downloadAudioFile(audio.source).onSuccess { result[audio.id] = it }
        }
        return result
    }

    private fun MigrationAudioMedia.withAudioFile(file: DownloadedAudioFile) = copy(
        audioFileName = file.fileNameWithExt,
        rawAudioDurationSeconds = file.durationSeconds ?: rawAudioDurationSeconds,
    )

    /** 다운로드 설정은 받는 기기 것을 덮는다. 대상 플레이리스트를 못 찾으면 미분류로 둔다(설정이 없을 때와 같다). */
    private suspend fun applyDownloadSettings(
        settings: MigrationDownloadSettings,
        plan: MigrationPlan,
        applied: MigrationApplyResult,
        target: MigrationSnapshot,
    ) {
        appSettingRepository.updateShouldInsertAtTopOnDownload(settings.shouldInsertAtTop).getOrThrow()
        val playlistId = when (val ref = settings.playlistId?.let { plan.playlistRefBySourceId[it] }) {
            is MigrationRef.Existing -> ref.id
            is MigrationRef.Incoming -> applied.newPlaylistIdBySourceId[ref.sourceId]
            null -> null
        } ?: target.playlists.first { it.type == PlaylistType.UNCATEGORIZED }.id
        appSettingRepository.updatePlaylistIdOnDownload(playlistId).getOrThrow()
    }
}
