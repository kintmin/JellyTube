package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.model.DownloadedAudioFile
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.data_migration.applyMigratedDownloadSettings
import com.kintmin.domain.data_migration.logText
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationChoice
import com.kintmin.domain.data_migration.model.MigrationDeclinedException
import com.kintmin.domain.data_migration.model.MigrationEndpoint
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationFileUnavailableException
import com.kintmin.domain.data_migration.model.MigrationPlan
import com.kintmin.domain.data_migration.model.MigrationPreview
import com.kintmin.domain.data_migration.model.MigrationProgress
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.model.MigrationStage
import com.kintmin.domain.data_migration.model.MigrationSummary
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.youtube.YoutubeUrl
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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
        confirm: suspend (MigrationPreview) -> MigrationChoice?,
    ): Result<MigrationSummary> = suspendRunCatching {
        onProgress(MigrationProgress(MigrationStage.PLANNING))
        // 계획은 받는 쪽 시스템 플레이리스트가 다 있어야 세울 수 있다.
        playlistRepository.ensureSystemPlaylists().getOrThrow()
        val export = dataMigrationRepository.fetchRemoteExport(endpoint, token).getOrThrow()
        val availableEntries = export.files.filter { MigrationFiles.isSafeName(it.file.name) }
        val available = MigrationFiles.keepAvailable(export.snapshot, availableEntries.map { it.file }.toSet())
        val target = dataMigrationRepository.fetchSnapshot().getOrThrow()

        // 목록은 이미 손에 있고 파일은 아직 받지 않았다. 묻기에 돈이 들지 않는 유일한 자리다.
        val choice = confirm(MigrationPlanner.preview(available, target)) ?: run {
            dataMigrationRepository.notifyDeclined(endpoint, token).onFailure { logError("받지 않기 알림 실패", it) }
            throw MigrationDeclinedException()
        }

        // 보내는 기기에 음원 파일이 없는 곡은 계획 전에 빠진다. 같은 곡의 다른 행으로 들어오거나 이미 이 기기에
        // 있으면 실패가 아니다.
        val targetKeys = target.audioMedias.map { SourceKey.of(it.source) }.toSet()
        val availableKeys = available.audioMedias.map { SourceKey.of(it.source) }.toSet()
        val missingOnSender = export.snapshot.audioMedias
            .filter { SourceKey.of(it.source) !in availableKeys }
            .filter { SourceKey.of(it.source) !in targetKeys }
            .distinctBy { SourceKey.of(it.source) }
        missingOnSender.forEach { log("보내는 기기에 음원 파일이 없음: ${it.logText()}") }

        val unplayable = MigrationPlanner.plan(available, target, choice).newAudioMedias
            .filterNot { dataMigrationRepository.isPlayableAudioFile(it.audioFileName) }
        val (redownloadable, unsupported) = unplayable.partition { YoutubeUrl.videoIdOf(it.source) != null }
        val sizeByFile = availableEntries.associate { it.file to it.size }
        unsupported.forEach {
            val size = sizeByFile[MigrationFile(MigrationFileKind.AUDIO, it.audioFileName)]
            log("재생할 수 없는 형식이라 들여오지 않음: ${it.logText(size)}")
        }
        val failures = (
            missingOnSender.map { MigrationFailure(it.name, MigrationFailureReason.TRANSFER_FAILED) } +
                unsupported.map { MigrationFailure(it.name, MigrationFailureReason.UNSUPPORTED_FORMAT) }
            ).toMutableList()
        val redownloaded = redownload(redownloadable, failures, onProgress)

        val applyPlan = try {
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
            val transferPlan = planWithout(source, target, choice, localFiles)

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
                choice,
                localFiles,
            )

            applyPlan
        } catch (e: Throwable) {
            // 반영 전에는 다시 받은 파일을 가리키는 행이 없어 두면 고아가 된다.
            withContext(NonCancellable) { deleteRedownloaded(redownloaded) }
            throw e
        }

        onProgress(MigrationProgress(MigrationStage.APPLY))
        // 커밋 직후 취소돼도 돌아오는 자리에서 CancellationException 이 난다. 반영 자체가 실패했을 때만 지운다.
        val applied = withContext(NonCancellable) {
            dataMigrationRepository.applyPlan(applyPlan).onFailure { deleteRedownloaded(redownloaded) }
        }.getOrThrow()

        // 반영이 끝났다. 다시 받은 파일은 이미 행이 가리키므로 여기부터는 실패해도 기록만 한다.
        dataMigrationRepository.clearStaging().onFailure { logError("임시 공간 정리 실패", it) }
        export.downloadSettings?.let { settings ->
            suspendRunCatching { appSettingRepository.applyMigratedDownloadSettings(settings, applyPlan, applied, target) }
                .onFailure { logError("다운로드 설정 반영 실패", it) }
        }
        MigrationSummary(
            newAudioMediaCount = applyPlan.newAudioMedias.size,
            newPlaylistCount = applyPlan.newPlaylists.size,
            redownloadedCount = redownloaded.size,
            failures = failures,
        ).also { summary ->
            log("이전 완료: ${summary.logText()}")
            dataMigrationRepository.notifyFinished(endpoint, token, summary)
                .onFailure { logError("완료 알림 실패", it) }
        }
    }.onFailure {
        if (it is MigrationDeclinedException) return@onFailure log("받는 사람이 확인 화면에서 받지 않기로 함")
        logError("이전 중단", it)
    }

    private suspend fun deleteRedownloaded(redownloaded: Map<Int, DownloadedAudioFile>) {
        redownloaded.values.forEach { audioMediaRepository.deleteFileAtFullPath(it.fileFullPath) }
    }

    private fun planWithout(
        source: MigrationSnapshot,
        target: MigrationSnapshot,
        choice: MigrationChoice,
        localFiles: Set<MigrationFile>,
    ) = MigrationPlanner.plan(source, target, choice).let { it.copy(requiredFiles = it.requiredFiles - localFiles) }

    /** 보내는 쪽 곡 id → 다시 받은 파일. 실패한 곡은 [failures] 에 더한다. */
    private suspend fun redownload(
        audioMedias: List<MigrationAudioMedia>,
        failures: MutableList<MigrationFailure>,
        onProgress: (MigrationProgress) -> Unit,
    ): Map<Int, DownloadedAudioFile> {
        val result = mutableMapOf<Int, DownloadedAudioFile>()
        try {
            audioMedias.forEachIndexed { index, audio ->
                onProgress(MigrationProgress(MigrationStage.REDOWNLOAD, doneCount = index, totalCount = audioMedias.size))
                audioMediaRepository.downloadAudioFile(audio.source)
                    .onSuccess { result[audio.id] = it }
                    .onFailure {
                        logError("다시 받기 실패: ${audio.logText()}", it)
                        failures += MigrationFailure(audio.name, MigrationFailureReason.REDOWNLOAD_FAILED)
                    }
            }
        } catch (e: Throwable) {
            // 돌려주기 전에 멈추면 호출자가 지울 목록을 받지 못한다.
            withContext(NonCancellable) { deleteRedownloaded(result) }
            throw e
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
                    val owner = plan.newAudioMedias.firstOrNull { MigrationFile(MigrationFileKind.AUDIO, it.audioFileName) == entry.file }
                    log("파일 받기 실패: ${entry.file} size=${entry.size} ${owner?.logText().orEmpty()} ${error.message ?: error::class.simpleName}")
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

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog("ImportMigrationDataUseCase", message))
    }

    private fun logError(message: String, error: Throwable) {
        appLog.sendErrorLog(ErrorLog("ImportMigrationDataUseCase", message, error))
    }
}
