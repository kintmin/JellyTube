package com.kintmin.domain.backup.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.ParallelDownloader
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.MigrationFiles
import com.kintmin.domain.data_migration.MigrationPlanner
import com.kintmin.domain.data_migration.SourceKey
import com.kintmin.domain.data_migration.applyMigratedDownloadSettings
import com.kintmin.domain.data_migration.logText
import com.kintmin.domain.data_migration.secretAudioMediaIds
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationFailure
import com.kintmin.domain.data_migration.model.MigrationFailureReason
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** 곡마다 반영하지 않고 끝에 한 번에 반영한다 — 여러 재생목록의 순서를 계획 그대로 맞추려면 그래야 한다. */
class RestoreBackupUseCase(
    private val dataMigrationRepository: DataMigrationRepository,
    private val playlistRepository: PlaylistRepository,
    private val appSettingRepository: AppSettingRepository,
    private val audioMediaRepository: AudioMediaRepository,
    private val backupRepository: BackupRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        backup: BackupContent,
        onProgress: (MigrationProgress) -> Unit,
    ): Result<MigrationSummary> = suspendRunCatching {
        onProgress(MigrationProgress(MigrationStage.PLANNING))
        // 계획은 이 기기에 시스템 플레이리스트가 다 있어야 세울 수 있다.
        playlistRepository.ensureSystemPlaylists().getOrThrow()
        val target = dataMigrationRepository.fetchSnapshot().getOrThrow()
        val source = backup.snapshot.withoutUnrecoverableFiles()
        val secretIds = source.secretAudioMediaIds()
        val newAudios = MigrationPlanner.plan(source, target).newAudioMedias

        val (redownloadable, notRestorable) = newAudios.partition { YoutubeUrl.videoIdOf(it.source) != null }
        notRestorable.forEach { log("원본이 유튜브가 아니라 복원하지 않음: ${it.logText(secretIds)}") }
        val failures = notRestorable.map { MigrationFailure(it.name, MigrationFailureReason.NOT_RESTORABLE) }.toMutableList()

        val staged = dataMigrationRepository.fetchStagedFiles().getOrThrow()
        val total = redownloadable.size
        onProgress(MigrationProgress(MigrationStage.REDOWNLOAD, doneCount = 0, totalCount = total))
        val results = ParallelDownloader.run(
            items = redownloadable,
            onDone = { onProgress(MigrationProgress(MigrationStage.REDOWNLOAD, doneCount = it, totalCount = total)) },
        ) { audio -> restore(audio, secretIds, staged, backup.lyrics) }
        val restored = mutableMapOf<Int, MigrationAudioMedia>()
        redownloadable.zip(results).forEach { (audio, result) ->
            result
                .onSuccess { restored[audio.id] = it }
                .onFailure {
                    logError("다시 받기 실패: ${audio.logText(secretIds)}", it)
                    failures += MigrationFailure(audio.name, MigrationFailureReason.REDOWNLOAD_FAILED)
                }
        }

        onProgress(MigrationProgress(MigrationStage.APPLY))
        // 같은 곡의 다른 행이 대표로 올라와 받지 못한 곡이 들어오지 않도록 키로 뺀다.
        val excludedKeys = newAudios.filter { it.id !in restored }.map { SourceKey.of(it.source) }.toSet()
        val plan = MigrationPlanner.plan(
            source.copy(
                audioMedias = source.audioMedias
                    .filter { SourceKey.of(it.source) !in excludedKeys }
                    .map { restored[it.id] ?: it },
            ),
            target,
        )
        // 반영과 그 뒤 정리는 한 덩어리로 끝낸다. 커밋 뒤에 끊기면 DB 는 바뀌었는데 설정과 임시 공간이 그대로 남는다.
        withContext(NonCancellable) {
            val applied = dataMigrationRepository.applyPlan(plan).getOrThrow()
            // 반영이 끝났다. 여기부터는 실패해도 기록만 한다.
            dataMigrationRepository.clearStaging().onFailure { logError("임시 공간 정리 실패", it) }
            backup.downloadSettings?.let { settings ->
                suspendRunCatching { appSettingRepository.applyMigratedDownloadSettings(settings, plan, applied, target) }
                    .onFailure { logError("다운로드 설정 반영 실패", it) }
            }
            adoptInstallIdIfFirstBackup(backup.installId)
        }
        MigrationSummary(
            newAudioMediaCount = plan.newAudioMedias.size,
            newPlaylistCount = plan.newPlaylists.size,
            redownloadedCount = restored.size,
            failures = failures,
        ).also { log("복원 완료: ${it.logText()}") }
    }.onFailure {
        logError("복원 중단", it)
    }

    /** 이미 백업하던 기기까지 물려받으면 공유 폴더를 쓰는 두 기기가 한 파일을 두고 번갈아 덮는다. */
    private suspend fun adoptInstallIdIfFirstBackup(installId: String?) {
        if (installId == null) return
        suspendRunCatching {
            if (backupRepository.getStatusFlow().first().lastSuccessMillis != null) return@suspendRunCatching
            backupRepository.adoptInstallId(installId).getOrThrow()
        }.onFailure { logError("설치 값 이어받기 실패", it) }
    }

    private suspend fun restore(
        audio: MigrationAudioMedia,
        secretIds: Set<Int>,
        staged: Set<MigrationFile>,
        lyrics: Map<String, String>,
    ): MigrationAudioMedia {
        val base = baseNameOf(audio.audioFileName) ?: "restore-${audio.id}"
        val stagedAudio = staged.firstOrNull { it.kind == MigrationFileKind.AUDIO && it.baseName == base }
        var duration = audio.rawAudioDurationSeconds
        val audioFileName = stagedAudio?.name ?: run {
            val downloaded = audioMediaRepository.downloadAudioFile(audio.source).getOrThrow()
            val ext = downloaded.fileNameWithExt.substringAfterLast('.', missingDelimiterValue = "")
            val name = if (ext.isEmpty()) base else "$base.$ext"
            try {
                dataMigrationRepository.stageLocalFile(downloaded.fileFullPath, MigrationFile(MigrationFileKind.AUDIO, name))
                    .getOrThrow()
            } catch (e: Throwable) {
                // 옮기기 전에 끊기면 받은 파일을 가리키는 행이 없어 두면 고아가 된다.
                withContext(NonCancellable) { audioMediaRepository.deleteFileAtFullPath(downloaded.fileFullPath) }
                throw e
            }
            duration = downloaded.durationSeconds ?: duration
            name
        }
        return audio.copy(
            audioFileName = audioFileName,
            rawAudioDurationSeconds = duration,
            imageFileName = audio.imageFileName?.let { stageThumbnail(audio, secretIds, it, staged) },
            lyricFileName = audio.lyricFileName?.let { stageLyric(it, lyrics, staged) },
        )
    }

    private suspend fun stageThumbnail(audio: MigrationAudioMedia, secretIds: Set<Int>, imageFileName: String, staged: Set<MigrationFile>): String? {
        val base = baseNameOf(imageFileName) ?: return null
        staged.firstOrNull { it.kind == MigrationFileKind.IMAGE && it.baseName == base }?.let { return it.name }
        val videoId = YoutubeUrl.videoIdOf(audio.source) ?: return null
        val savedName = audioMediaRepository.saveThumbnail(thumbnailUrlOf(videoId), base)
            .getOrElse { error -> return skip(error, "썸네일 받기 실패: ${audio.logText(secretIds)}") }
        val file = MigrationFile(MigrationFileKind.IMAGE, savedName)
        val path = dataMigrationRepository.resolveFilePath(file).getOrElse { error -> return skip(error, "썸네일 경로 실패: $savedName") }
        dataMigrationRepository.stageLocalFile(path, file).getOrElse { error ->
            withContext(NonCancellable) { audioMediaRepository.deleteFileAtFullPath(path) }
            return skip(error, "썸네일 옮기기 실패: $savedName")
        }
        return savedName
    }

    private suspend fun stageLyric(lyricFileName: String, lyrics: Map<String, String>, staged: Set<MigrationFile>): String? {
        if (!MigrationFiles.isSafeName(lyricFileName)) return null
        val file = MigrationFile(MigrationFileKind.LYRIC, lyricFileName)
        if (file in staged) return lyricFileName
        val text = lyrics[lyricFileName] ?: return null
        dataMigrationRepository.stageText(file, text).getOrElse { error -> return skip(error, "가사 쓰기 실패: $lyricFileName") }
        return lyricFileName
    }

    private fun skip(error: Throwable, message: String): String? {
        logError(message, error)
        return null
    }

    /** 백업에 적힌 이름은 이 기기에서 만들 파일 이름이 된다. 디렉터리를 벗어나는 이름은 쓰지 않는다. */
    private fun baseNameOf(fileName: String): String? =
        fileName.takeIf(MigrationFiles::isSafeName)?.substringBeforeLast('.')?.ifEmpty { null }

    private val MigrationFile.baseName get() = name.substringBeforeLast('.')

    private fun thumbnailUrlOf(videoId: String) = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    /** 편집본과 커스텀 이미지는 백업에 없고 원본에서 다시 받을 수도 없다. */
    private fun MigrationSnapshot.withoutUnrecoverableFiles() = copy(
        audioMedias = audioMedias.map { it.copy(editedAudioFileName = null, editedAudioDurationSeconds = null) },
        playlists = playlists.map { it.copy(customImageFileName = null) },
    )

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog("RestoreBackupUseCase", message))
    }

    private fun logError(message: String, error: Throwable) {
        appLog.sendErrorLog(ErrorLog("RestoreBackupUseCase", message, error))
    }
}
