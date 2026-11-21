package com.kintmin.domain.backup

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.backup.usecase.FindBackupToRestoreUseCase
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindBackupToRestoreUseCaseTest {

    private fun backup(createdAt: Long) = BackupContent(
        MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), null, emptyMap(), createdAt,
    )

    private fun useCase(local: Result<BackupContent?>, folder: Result<BackupContent?>) = FindBackupToRestoreUseCase(
        mockk<BackupRepository> {
            coEvery { readLocalCopy() } returns local
            coEvery { readFolderCopy() } returns folder
        },
    )

    @Test
    fun `둘 다 있으면 더 최근 백업을 고른다`() = runTest {
        assertEquals(2L, useCase(Result.success(backup(1)), Result.success(backup(2)))().getOrThrow()?.createdAtMillis)
        assertEquals(3L, useCase(Result.success(backup(3)), Result.success(backup(2)))().getOrThrow()?.createdAtMillis)
    }

    @Test
    fun `한쪽을 읽지 못해도 다른 쪽이 있으면 그것을 쓴다`() = runTest {
        val found = useCase(Result.failure(BackupFileInvalidException()), Result.success(backup(5)))().getOrThrow()

        assertEquals(5L, found?.createdAtMillis)
    }

    @Test
    fun `둘 다 없으면 null 이고 둘 다 못 읽으면 실패다`() = runTest {
        assertNull(useCase(Result.success(null), Result.success(null))().getOrThrow())
        assertTrue(useCase(Result.failure(BackupFileInvalidException()), Result.success(null))().isFailure)
    }
}
