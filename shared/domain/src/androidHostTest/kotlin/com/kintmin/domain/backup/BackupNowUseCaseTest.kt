package com.kintmin.domain.backup

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupFolderNotRegisteredException
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.model.BackupWriteResult
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.backup.usecase.BackupNowUseCase
import com.kintmin.domain.data_migration.model.MigrationAudioMedia
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupNowUseCaseTest {

    private val folder = BackupFolder("content://tree/1", "Download")
    private val written = slot<BackupContent>()
    private val backupRepository = mockk<BackupRepository> {
        every { getStatusFlow() } returns flowOf(BackupStatus(folder, null, null, null))
        coEvery { write(capture(written)) } returns Result.success(BackupWriteResult(100, localCopySkipped = false))
        coEvery { recordSuccess(any()) } returns Result.success(Unit)
        coEvery { recordFailure(any(), any()) } returns Result.success(Unit)
    }
    private val dataMigrationRepository = mockk<DataMigrationRepository> {
        every { resolveFilePath(any()) } answers { Result.success("/lyrics/${firstArg<MigrationFile>().name}") }
    }
    private val audioMediaRepository = mockk<AudioMediaRepository> {
        coEvery { getLyrics("/lyrics/l1.lrc") } returns Result.success("가사")
        coEvery { getLyrics("/lyrics/gone.lrc") } returns Result.failure(IllegalStateException("없음"))
    }
    private val appSettingRepository = mockk<AppSettingRepository> {
        every { getShouldInsertAtTopOnDownloadFlow() } returns flowOf(true)
        every { getPlaylistIdOnDownloadFlow() } returns flowOf(5)
    }
    private val appLog = mockk<AppLog>(relaxed = true)
    private val useCase = BackupNowUseCase(backupRepository, dataMigrationRepository, audioMediaRepository, appSettingRepository, appLog)

    private fun audio(id: Int, lyric: String? = null) = MigrationAudioMedia(
        id = id, source = "https://youtu.be/$id", name = "곡$id", artist = "", description = "", rawAudioDurationSeconds = 1,
        audioFileName = "$id.m4a", imageFileName = null, lyricFileName = lyric, tjKaraokeNumber = null,
        editedAudioFileName = null, editedAudioDurationSeconds = null, createdTimeMillis = id.toLong(),
    )

    private fun snapshot(vararg audios: MigrationAudioMedia) =
        MigrationSnapshot(audios.toList(), emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun `음원이 하나도 없으면 남아 있던 백업을 덮지 않는다`() = runTest {
        coEvery { dataMigrationRepository.fetchSnapshot() } returns Result.success(snapshot())

        assertEquals(BackupOutcome.SKIPPED_EMPTY, useCase(1_000).getOrThrow())
        coVerify(exactly = 0) { backupRepository.write(any()) }
        coVerify(exactly = 0) { backupRepository.recordSuccess(any()) }
    }

    @Test
    fun `읽히는 가사와 다운로드 설정을 담아 쓰고 성공을 적는다`() = runTest {
        coEvery { dataMigrationRepository.fetchSnapshot() } returns
            Result.success(snapshot(audio(1, lyric = "l1.lrc"), audio(2, lyric = "gone.lrc"), audio(3)))

        assertEquals(BackupOutcome.WRITTEN, useCase(1_000).getOrThrow())
        assertEquals(mapOf("l1.lrc" to "가사"), written.captured.lyrics)
        assertEquals(MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 5), written.captured.downloadSettings)
        assertEquals(1_000L, written.captured.createdAtMillis)
        coVerify { backupRepository.recordSuccess(1_000) }
    }

    @Test
    fun `폴더에 접근할 수 없으면 그 사유로 실패를 적는다`() = runTest {
        coEvery { dataMigrationRepository.fetchSnapshot() } returns Result.success(snapshot(audio(1)))
        coEvery { backupRepository.write(any()) } returns Result.failure(BackupFolderUnavailableException())

        assertTrue(useCase(1_000).exceptionOrNull() is BackupFolderUnavailableException)
        coVerify { backupRepository.recordFailure(1_000, BackupFailureReason.FOLDER_UNAVAILABLE) }
        coVerify(exactly = 0) { backupRepository.recordSuccess(any()) }
    }

    @Test
    fun `폴더를 고르지 않았으면 쓰지 않는다`() = runTest {
        every { backupRepository.getStatusFlow() } returns flowOf(BackupStatus(null, null, null, null))

        assertTrue(useCase(1_000).exceptionOrNull() is BackupFolderNotRegisteredException)
    }
}
