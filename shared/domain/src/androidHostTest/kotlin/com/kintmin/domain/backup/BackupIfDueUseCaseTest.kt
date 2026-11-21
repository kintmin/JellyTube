package com.kintmin.domain.backup

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupOutcome
import com.kintmin.domain.backup.model.BackupStatus
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.backup.usecase.BackupIfDueUseCase
import com.kintmin.domain.backup.usecase.BackupNowUseCase
import com.kintmin.domain.backup.usecase.RegisterBackupFolderUseCase
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupIfDueUseCaseTest {

    private val folder = BackupFolder("downloads", "Download/JellyTube")
    private val now = Instant.parse("2026-11-21T12:00:00Z").toEpochMilliseconds()
    private val yesterday = Instant.parse("2026-11-20T12:00:00Z").toEpochMilliseconds()
    private val earlierToday = Instant.parse("2026-11-21T01:00:00Z").toEpochMilliseconds()
    private val backup = BackupContent(MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), null, emptyMap(), 0)

    private val backupRepository = mockk<BackupRepository> {
        coEvery { hasLocalCopy() } returns false
        coEvery { readFolderCopy() } returns Result.success(null)
        coEvery { registerFolder(any()) } returns Result.success(Unit)
    }
    private val dataMigrationRepository = mockk<DataMigrationRepository> {
        coEvery { fetchStagedFiles() } returns Result.success(emptySet())
    }
    private val backupNow = mockk<BackupNowUseCase> {
        coEvery { this@mockk(any()) } returns Result.success(BackupOutcome.WRITTEN)
    }
    private val useCase = BackupIfDueUseCase(backupRepository, dataMigrationRepository, backupNow)

    private fun status(
        folder: BackupFolder? = this.folder,
        lastSuccess: Long? = null,
        lastFailure: Long? = null,
    ) = every { backupRepository.getStatusFlow() } returns flowOf(
        BackupStatus(folder, lastSuccess, lastFailure, lastFailure?.let { BackupFailureReason.FOLDER_UNAVAILABLE }),
    )

    private suspend fun run() = useCase(now, TimeZone.UTC).getOrThrow()

    @Test
    fun `처음 설치한 기기는 날짜가 바뀐 첫 기회에 쓴다`() = runTest {
        status()

        assertEquals(BackupOutcome.WRITTEN, run())
    }

    @Test
    fun `오늘 이미 썼거나 폴더가 없으면 쓰지 않는다`() = runTest {
        status(lastSuccess = earlierToday)
        assertNull(run())

        status(folder = null)
        assertNull(run())

        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `폴더 백업을 읽지 못하면 복원을 기다리는 것으로 보고 쓰지 않는다`() = runTest {
        status()
        coEvery { backupRepository.readFolderCopy() } returns Result.failure(Exception("provider unavailable"))

        assertNull(run())
        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `받아 둔 이전 파일을 읽지 못하면 복원을 기다리는 것으로 보고 쓰지 않는다`() = runTest {
        status(lastSuccess = yesterday)
        coEvery { dataMigrationRepository.fetchStagedFiles() } returns Result.failure(Exception("io"))

        assertNull(run())
        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `재설치로 앱 안 사본만 되살아났으면 복원을 기다리며 쓰지 않는다`() = runTest {
        status()
        coEvery { backupRepository.hasLocalCopy() } returns true

        assertNull(run())
        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `폴더에만 남은 백업이 있어도 쓰지 않는다`() = runTest {
        status()
        coEvery { backupRepository.readFolderCopy() } returns Result.success(backup)

        assertNull(run())
    }

    @Test
    fun `한 번이라도 백업한 설치는 남은 백업이 있어도 날마다 쓴다`() = runTest {
        status(lastSuccess = yesterday)
        coEvery { backupRepository.hasLocalCopy() } returns true

        assertEquals(BackupOutcome.WRITTEN, run())
    }

    @Test
    fun `이전이나 복원이 받아 둔 파일이 남아 있으면 이어 받을 수 있게 쓰지 않는다`() = runTest {
        status(lastSuccess = yesterday)
        coEvery { dataMigrationRepository.fetchStagedFiles() } returns
            Result.success(setOf(MigrationFile(MigrationFileKind.AUDIO, "a.m4a")))

        assertNull(run())
    }

    @Test
    fun `오늘 이미 실패했으면 앱이 내려갈 때마다 다시 만들지 않고 어제 실패면 다시 쓴다`() = runTest {
        status(lastSuccess = yesterday, lastFailure = earlierToday)
        assertNull(run())

        status(lastSuccess = null, lastFailure = yesterday)
        assertEquals(BackupOutcome.WRITTEN, run())
    }

    @Test
    fun `재설치 뒤 고른 폴더의 백업을 읽지 못하면 덮지 않는다`() = runTest {
        status()
        coEvery { backupRepository.readFolderCopy() } returns Result.failure(Exception("provider unavailable"))

        val outcome = RegisterBackupFolderUseCase(backupRepository, backupNow, mockk(relaxed = true))(folder, now).getOrThrow()

        assertEquals(BackupOutcome.KEPT_EXISTING, outcome)
        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `재설치 뒤 백업이 든 폴더를 다시 고르면 그 백업을 덮지 않는다`() = runTest {
        status()
        coEvery { backupRepository.readFolderCopy() } returns Result.success(backup)

        val outcome = RegisterBackupFolderUseCase(backupRepository, backupNow, mockk(relaxed = true))(folder, now).getOrThrow()

        assertEquals(BackupOutcome.KEPT_EXISTING, outcome)
        coVerify(exactly = 0) { backupNow(any()) }
    }

    @Test
    fun `백업하던 설치가 폴더를 바꾸면 새 폴더에 바로 쓴다`() = runTest {
        status(lastSuccess = yesterday)
        coEvery { backupRepository.readFolderCopy() } returns Result.success(backup)

        val outcome = RegisterBackupFolderUseCase(backupRepository, backupNow, mockk(relaxed = true))(folder, now).getOrThrow()

        assertEquals(BackupOutcome.WRITTEN, outcome)
    }
}
