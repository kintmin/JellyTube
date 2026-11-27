package com.kintmin.domain.backup

import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFileInvalidException
import com.kintmin.domain.backup.repository.BackupRepository
import com.kintmin.domain.backup.usecase.FindBackupToRestoreUseCase
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import io.mockk.coEvery
import com.kintmin.log.AppLog
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FindBackupToRestoreUseCaseTest {

    private fun backup(createdAt: Long) = BackupContent(
        MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), null, emptyMap(), createdAt,
    )

    private val appLog = mockk<AppLog>(relaxed = true)

    private fun useCase(folder: Result<BackupContent?>) = FindBackupToRestoreUseCase(
        mockk<BackupRepository> { coEvery { readFolderCopy() } returns folder },
        appLog,
    )

    @Test
    fun `폴더에서 고른 백업을 그대로 돌려준다`() = runTest {
        assertEquals(5L, useCase(Result.success(backup(5)))().getOrThrow()?.createdAtMillis)
    }

    @Test
    fun `폴더에 백업이 없으면 null 이다`() = runTest {
        assertNull(useCase(Result.success(null))().getOrThrow())
    }

    @Test
    fun `읽지 못한 것을 없는 것으로 삼키지 않고 올린다`() = runTest {
        val result = useCase(Result.failure(BackupFileInvalidException()))()

        assertTrue(result.isFailure)
        verify { appLog.sendErrorLog(match { it.message == "폴더 백업 읽기 실패" && it.throwable is BackupFileInvalidException }) }
    }
}
