package com.kintmin.data.repository_impl

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupInstallIdTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun repository() = BackupRepositoryImpl(
        datastoreUtil = mockk(),
        fileManager = mockk { every { getInstallIdDirPath() } returns Result.success(tempFolder.root.absolutePath) },
        backupFolderAccess = mockk(),
    )

    @Test
    fun `처음 물으면 만들어 두고 그 뒤에는 같은 값을 돌려준다`() = runTest {
        val first = repository().getInstallId().getOrThrow()

        assertTrue(first.isNotBlank())
        // 새 인스턴스가 파일에서 읽는다. 앱을 다시 켜도 같은 값이어야 백업 파일 계보가 이어진다.
        assertEquals(first, repository().getInstallId().getOrThrow())
    }

    @Test
    fun `되돌린 백업의 값을 물려받으면 그 뒤로 그 값을 쓴다`() = runTest {
        val repository = repository()
        repository.getInstallId().getOrThrow()

        repository.adoptInstallId("이전-설치").getOrThrow()

        assertEquals("이전-설치", repository.getInstallId().getOrThrow())
    }
}
