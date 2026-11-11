package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.data_migration.model.MigrationApplyResult
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationFileUnavailableException
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationRef
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.youtube.YoutubeUrl
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 보내는 기기의 데이터를 받아 이 기기에 합친다. 곡 하나의 실패는 그 곡을 빼고 계속하고
 * ([MigrationSummary.failures]), 그 밖의 실패는 전체를 멈춘다. 다시 부르면 받아 둔 파일은 건너뛰고
 * 빠진 곡만 이어 받는다.
 */
class ImportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
    private val playlistRepository: PlaylistRepository,
    private val appSettingRepository: AppSettingRepository,
    private val audioMediaRepository: AudioMediaRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        endpoint: MigrationEndpoint,
        token: String,
        onProgress: (MigrationProgress) -> Unit,
    ): Result<MigrationSummary> = runCatching {
        onProgress(MigrationProgress(MigrationStage.PLANNING))
        // 계획은 받는 쪽 시스템 플레이리스트가 다 있어야 세울 수 있다.
        playlistRepository.ensureSystemPlaylists().getOrThrow()
        val export = dataMigrationRepository.fetchRemoteExport(endpoint, token).getOrThrow()
        val availableEntries = export.files.filter { MigrationFiles.isSafeName(it.file.name) }
        val available = MigrationFiles.keepAvailable(export.snapshot, availableEntries.map { it.file }.toSet())
        val target = dataMigrationRepository.fetchSnapshot().getOrThrow()

        // 보내는 기기에 음원 파일이 없는 곡은 계획 전에 빠진다. 이미 이 기기에 있는 곡이 아니면 실패로 알린다.
        val targetKeys = target.audioMedias.map { SourceKey.of(it.source) }.toSet()
        val availableIds = available.audioMedias.map { it.id }.toSet()
        val missingOnSender = export.snapshot.audioMedias
            .filter { it.id !in availableIds }
            .filter { SourceKey.of(it.source) !in targetKeys }
            .distinctBy { SourceKey.of(it.source) }
        missingOnSender.forEach { log("보내는 기기에 음원 파일이 없음: ${it.name} (${it.audioFileName})") }

        val unplayable = MigrationPlanner.plan(available, target).newAudioMedias
            .filterNot { dataMigrationRepository.isPlayableAudioFile(it.audioFileName) }
        val (redownloadable, unsupported) = unplayable.partition { YoutubeUrl.videoIdOf(it.source) != null }
        unsupported.forEach { log("재생할 수 없는 형식이라 들여오지 않음: ${it.name} (${it.audioFileName})") }
        val failures = (
            missingOnSender.map { MigrationFailure(it.name, MigrationFailureReason.TRANSFER_FAILED) } +
                unsupported.map { MigrationFailure(it.name, MigrationFailureReason.UNSUPPORTED_FORMAT) }
            ).toMutableList()
        val redownloaded = redownload(redownloadable, failures, onProgress)

        val (applyPlan, applied) = try {
            // 같은 곡의 다른 행이 대표로 올라와 빠진 곡의 파일을 가져오지 않도록 키로 뺀다.
            val excludedKeys = unplayable.filter { it.id !in redownloaded }
                .map { SourceKey.of(it.source) }
                .toMutableSet()
            val source = available.copy(
                audioMedias = available.audioMedias
                    .filter { SourceKey.of(it.source) !in excludedKeys }
                    .map { audio -> redownloaded[audio.id]?.let { audio.withAudioFile(it) } ?: audio },
            )
            val localFiles = redownloaded.values
                .map { MigrationFile(MigrationFileKind.AUDIO, it.fileNameWithExt) }
                .toSet()
            val transferPlan = planWithout(source, target, localFiles)

            val unavailableFiles = transfer(endpoint, token, availableEntries, transferPlan, onProgress)

            // 받지 못한 파일이 음원이면 그 곡을 빼고, 썸네일·가사·편집본이면 그 값만 비운다.
            transferPlan.newAudioMedias
                .filter { MigrationFile(MigrationFileKind.AUDIO, it.audioFileName) in unavailableFiles }
                .forEach {
                    excludedKeys += SourceKey.of(it.source)
                    failures += MigrationFailure(it.name, MigrationFailureReason.TRANSFER_FAILED)
                }
            val receivedFiles = availableEntries.map { it.file }.toSet() - unavailableFiles + localFiles
            val applyPlan = planWithout(
                MigrationFiles.keepAvailable(
                    source.copy(audioMedias = source.audioMedias.filter { SourceKey.of(it.source) !in excludedKeys }),
                    receivedFiles,
                ),
                target,
                localFiles,
            )

            onProgress(MigrationProgress(MigrationStage.APPLY))
            applyPlan to dataMigrationRepository.applyPlan(applyPlan).getOrThrow()
        } catch (e: Throwable) {
            // 반영 전에는 다시 받은 파일을 가리키는 행이 없어 두면 고아가 된다.
            withContext(NonCancellable) {
                redownloaded.values.forEach { audioMediaRepository.deleteFileAtFullPath(it.fileFullPath) }
            }
            throw e
        }

        // 반영이 끝났다. 다시 받은 파일은 이미 행이 가리키므로 여기부터는 실패해도 기록만 한다.
        dataMigrationRepository.clearStaging().onFailure { log("임시 공간 정리 실패: $it") }
        export.downloadSettings?.let { settings ->
            runCatching { applyDownloadSettings(settings, applyPlan, applied, target) }
                .onFailure { log("다운로드 설정 반영 실패: $it") }
        }
        MigrationSummary(
            newAudioMediaCount = applyPlan.newAudioMedias.size,
            newPlaylistCount = applyPlan.newPlaylists.size,
            redownloadedCount = redownloaded.size,
            failures = failures,
        ).also { summary ->
            dataMigrationRepository.notifyFinished(endpoint, token, summary)
                .onFailure { log("완료 알림 실패: $it") }
        }
    }.onFailure {
        if (it is CancellationException) throw it
        log("이전 중단: $it")
    }

    private fun planWithout(source: MigrationSnapshot, target: MigrationSnapshot, localFiles: Set<MigrationFile>) =
        MigrationPlanner.plan(source, target).let { it.copy(requiredFiles = it.requiredFiles - localFiles) }

    /** 보내는 쪽 곡 id → 다시 받은 파일. 실패한 곡은 [failures] 에 더한다. */
    private suspend fun redownload(
        audioMedias: List<MigrationAudioMedia>,
        failures: MutableList<MigrationFailure>,
        onProgress: (MigrationProgress) -> Unit,
    ): Map<Int, DownloadedAudioFile> {
        val result = mutableMapOf<Int, DownloadedAudioFile>()
        audioMedias.forEachIndexed { index, audio ->
            onProgress(MigrationProgress(MigrationStage.REDOWNLOAD, doneCount = index, totalCount = audioMedias.size))
            audioMediaRepository.downloadAudioFile(audio.source)
                .onSuccess { result[audio.id] = it }
                .onFailure {
                    if (it is CancellationException) throw it
                    log("다시 받기 실패: ${audio.name} (${audio.source}) $it")
                    failures += MigrationFailure(audio.name, MigrationFailureReason.REDOWNLOAD_FAILED)
                }
        }
        onProgress(MigrationProgress(MigrationStage.REDOWNLOAD, audioMedias.size, audioMedias.size))
        return result
    }

    /** 받지 못한 파일을 돌려준다. 그 파일 하나의 문제가 아니면 던져서 전체를 멈춘다. */
    private suspend fun transfer(
        endpoint: MigrationEndpoint,
        token: String,
        availableEntries: List<MigrationFileEntry>,
        plan: MigrationPlan,
        onProgress: (MigrationProgress) -> Unit,
    ): Set<MigrationFile> {
        val entries = availableEntries.filter { it.file in plan.requiredFiles }
        val totalBytes = entries.sumOf { it.size }
        var receivedBytes = 0L
        val unavailable = mutableSetOf<MigrationFile>()
        // 청크마다 알리면 수 GB 에서 화면을 수만 번 다시 그린다.
        val reportStepBytes = maxOf(totalBytes / 100, 1)
        var reportedBytes = 0L
        fun report(doneCount: Int, received: Long, force: Boolean = false) {
            if (!force && received - reportedBytes < reportStepBytes) return
            reportedBytes = received
            onProgress(MigrationProgress(MigrationStage.TRANSFER, doneCount, entries.size, received, totalBytes))
        }

        report(0, 0, force = true)
        entries.forEachIndexed { index, entry ->
            val before = receivedBytes
            dataMigrationRepository.stageFile(endpoint, token, entry) { received -> report(index, before + received) }
                .onFailure { error ->
                    if (error !is MigrationFileUnavailableException) throw error
                    log("파일 받기 실패: ${entry.file} $error")
                    unavailable += entry.file
                }
            receivedBytes = before + entry.size
            report(index + 1, receivedBytes, force = true)
        }
        return unavailable
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

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog("ImportMigrationDataUseCase", message))
    }
}
