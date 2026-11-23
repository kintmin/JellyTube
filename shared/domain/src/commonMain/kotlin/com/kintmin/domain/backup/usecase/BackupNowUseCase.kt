package com.kintmin.domain.backup.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolderNotRegisteredException
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class BackupNowUseCase(
    private val backupRepository: BackupRepository,
    private val dataMigrationRepository: DataMigrationRepository,
    private val audioMediaRepository: AudioMediaRepository,
    private val appSettingRepository: AppSettingRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(nowMillis: Long): Result<BackupOutcome> = suspendRunCatching {
        if (backupRepository.getStatusFlow().first().folder == null) throw BackupFolderNotRegisteredException()
        val snapshot = dataMigrationRepository.fetchSnapshot().getOrThrow()
        if (snapshot.audioMedias.isEmpty()) {
            log("음원이 없어 백업을 쓰지 않음")
            return@suspendRunCatching BackupOutcome.SKIPPED_EMPTY
        }
        val lyrics = snapshot.audioMedias.mapNotNull { it.lyricFileName }.distinct().mapNotNull { name ->
            val path = dataMigrationRepository.resolveFilePath(MigrationFile(MigrationFileKind.LYRIC, name)).getOrNull()
            val text = path?.let { audioMediaRepository.getLyrics(it).getOrNull() }
            if (text == null) log("가사 파일을 읽지 못해 백업에서 뺌: $name")
            text?.let { name to it }
        }.toMap()
        val content = BackupContent(
            snapshot = snapshot,
            downloadSettings = MigrationDownloadSettings(
                shouldInsertAtTop = appSettingRepository.getShouldInsertAtTopOnDownloadFlow().first(),
                playlistId = appSettingRepository.getPlaylistIdOnDownloadFlow().first(),
            ),
            lyrics = lyrics,
            createdAtMillis = nowMillis,
        )
        val written = backupRepository.write(content).getOrElse { error ->
            val reason = if (error is BackupFolderUnavailableException) {
                BackupFailureReason.FOLDER_UNAVAILABLE
            } else {
                BackupFailureReason.WRITE_FAILED
            }
            backupRepository.recordFailure(nowMillis, reason)
            throw error
        }
        if (written.localCopySkipped) log("백업이 ${written.sizeBytes}B 라 클라우드 자동 백업용 사본은 쓰지 않음")
        backupRepository.recordSuccess(nowMillis).getOrThrow()
        log("백업 완료: ${written.sizeBytes}B")
        BackupOutcome.WRITTEN
    }.onFailure {
        if (it is CancellationException) throw it
        logError("백업 실패", it)
    }

    private fun log(message: String) {
        appLog.sendDebugLog(DebugLog("BackupNowUseCase", message))
    }

    private fun logError(message: String, error: Throwable) {
        appLog.sendErrorLog(ErrorLog("BackupNowUseCase", message, error))
    }
}
