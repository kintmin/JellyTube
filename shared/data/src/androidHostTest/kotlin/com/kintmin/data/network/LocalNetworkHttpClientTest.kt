package com.kintmin.data.network

import com.kintmin.data.file_share.KeepAliveByForegroundService
import com.kintmin.data.file_share.fileShareModule
import com.kintmin.data.file_share.migration.MigrationSession
import com.kintmin.data.network.data_source_impl.MigrationDataSourceImpl
import com.kintmin.domain.data_migration.MigrationSendHandler
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

/** 실제 소켓으로 보내는 서버에 평문 HTTP 로 붙는다. Android 에서 이전 클라이언트가 CIO 인 이유를 고정한다. */
class LocalNetworkHttpClientTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `CIO 클라이언트로 페어링하고 manifest 와 큰 파일을 받는다`() = runBlocking {
        val audio = tempFolder.newFile("a.m4a").apply { writeBytes(Random(1).nextBytes(300_000)) }
        val session = MigrationSession(code = "123456", generateToken = { "token" })
        val handler = object : MigrationSendHandler {
            override suspend fun cleanup() = Result.success(Unit)

            override suspend fun export() = Result.success(
                MigrationExport(
                    snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                    files = listOf(MigrationFileEntry(MigrationFile(MigrationFileKind.AUDIO, "a.m4a"), audio.length())),
                    downloadSettings = null,
                ),
            )

            override fun resolveFilePath(file: MigrationFile) = Result.success(audio.path)
        }
        val server = embeddedServer(CIO, port = 0) {
            fileShareModule(
                pcUploadHandler = mockk(),
                uploadTempDir = tempFolder.root.path,
                receiveKeepAlive = KeepAliveByForegroundService,
                currentMigrationSession = { session },
                migrationSendHandler = handler,
            )
        }
        server.startSuspend(wait = false)
        val client = createLocalNetworkHttpClient()
        try {
            val port = server.engine.resolvedConnectors().first().port
            val dataSource = MigrationDataSourceImpl(client) { "Galaxy" }

            val token = dataSource.pair("127.0.0.1", port, "123456").getOrThrow()
            val manifest = dataSource.fetchManifest("127.0.0.1", port, token).getOrThrow()
            val received = java.io.ByteArrayOutputStream()
            dataSource.downloadFile("127.0.0.1", port, token, "AUDIO", "a.m4a") { bytes, count -> received.write(bytes, 0, count) }
                .getOrThrow()

            assertEquals("token", token)
            assertEquals(1, manifest.files.size)
            assertEquals(audio.length(), manifest.files.single().size)
            assertTrue(audio.readBytes().contentEquals(received.toByteArray()))
        } finally {
            client.close()
            server.stopSuspend(0, 0)
        }
    }
}
