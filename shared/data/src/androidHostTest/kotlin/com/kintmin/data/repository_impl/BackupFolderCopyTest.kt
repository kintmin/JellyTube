package com.kintmin.data.repository_impl

import com.kintmin.data.backup.BackupCodec
import com.kintmin.data.backup.BackupFileName
import com.kintmin.data.backup.BackupFolderAccess
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.domain.backup.model.BackupContent
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import com.kintmin.domain.backup.model.BackupVersionUnsupportedException
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Sink
import okio.buffer
import okio.gzip
import okio.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupFolderCopyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val day = 24 * 60 * 60 * 1000L
    private val today = 1_764_201_600_000
    private val twoDaysAgo = today - 2 * day

    private fun content(createdAtMillis: Long) = BackupContent(
        MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        downloadSettings = null,
        lyrics = emptyMap(),
        createdAtMillis = createdAtMillis,
    )

    private fun repository(access: BackupFolderAccess) = BackupRepositoryImpl(
        datastoreUtil = mockk { every { getData(PreferencesKey.BackupFolderId) } returns flowOf("tree") },
        fileManager = mockk { every { getInstallIdDirPath() } returns Result.success(tempFolder.root.absolutePath) },
        backupFolderAccess = access,
    )

    @Test
    fun `같은 설치가 전에 쓴 것만 지우고 옛 이름은 둔다`() = runTest {
        val access = FakeFolderAccess(mutableMapOf("jellytube-backup.json.gz" to BackupCodec.encode(content(0))))
        val repository = repository(access)
        repository.write(content(twoDaysAgo)).getOrThrow()

        repository.write(content(today)).getOrThrow()

        val names = access.files.keys
        assertEquals(1, names.count { BackupFileName.parse(it) != null })
        assertTrue("옛 이름은 누가 썼는지 알 수 없어 둔다", "jellytube-backup.json.gz" in names)
    }

    @Test
    fun `치우다 실패해도 백업은 성공이다`() = runTest {
        val access = FakeFolderAccess(failOnDelete = true)
        val repository = repository(access)
        repository.write(content(twoDaysAgo)).getOrThrow()

        // 백업은 이미 폴더에 들어갔다. 치우기가 막혔다고 실패로 적으면 그날 다시 쓰지 않는다.
        assertTrue(repository.write(content(today)).isSuccess)
    }

    @Test
    fun `더 새 버전이 있으면 옛 백업으로 내려가지 않고 그대로 알린다`() = runTest {
        val newer = gzip("""{"backupVersion":${BackupCodec.FORMAT_VERSION + 1},"createdAtMillis":0,"snapshot":{}}""")
        val access = FakeFolderAccess(
            mutableMapOf(
                "JellyTube_20261127_새설치.json.gz" to newer,
                "JellyTube_20261120_옛설치.json.gz" to BackupCodec.encode(content(0)),
            ),
        )

        val result = repository(access).readFolderCopy()

        assertTrue(result.exceptionOrNull() is BackupVersionUnsupportedException)
    }

    @Test
    fun `읽히는 것이 하나도 없으면 없다고 하지 않고 첫 실패를 올린다`() = runTest {
        val access = FakeFolderAccess(mutableMapOf("JellyTube_20261127_설치.json.gz" to "백업이 아님".encodeToByteArray()))

        val result = repository(access).readFolderCopy()

        assertTrue(result.isFailure)
    }

    @Test
    fun `폴더에 백업이 없으면 null 이다`() = runTest {
        val access = FakeFolderAccess(mutableMapOf("노래.mp3" to byteArrayOf(1)))

        assertEquals(null, repository(access).readFolderCopy().getOrThrow())
    }

    private fun gzip(text: String): ByteArray {
        val buffer = Buffer()
        (buffer as Sink).gzip().buffer().use { it.writeUtf8(text) }
        return buffer.readByteArray()
    }

    private class FakeFolderAccess(
        val files: MutableMap<String, ByteArray> = mutableMapOf(),
        private val failOnDelete: Boolean = false,
    ) : BackupFolderAccess {

        override suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String? {
            files[fileName] = bytes
            return null
        }

        override suspend fun list(folderId: String) = files.keys.toList()

        override suspend fun read(folderId: String, fileName: String) = files[fileName]

        override suspend fun delete(folderId: String, fileName: String) {
            if (failOnDelete) throw BackupFolderUnavailableException()
            files -= fileName
        }
    }
}
