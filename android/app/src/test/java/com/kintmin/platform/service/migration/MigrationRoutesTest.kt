package com.kintmin.platform.service.migration

import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationFile
import com.kintmin.domain.data_migration.model.MigrationFileEntry
import com.kintmin.domain.data_migration.model.MigrationFileKind
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.ResolveMigrationFilePathUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.MigrationFinishRequest
import com.kintmin.platform.service.fileShareModule
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import com.kintmin.domain.audio_media.usecase.CleanupAnomalousDataUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MigrationRoutesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val cleanupUseCase = mockk<CleanupAnomalousDataUseCase> {
        coEvery { this@mockk() } returns Result.success(0)
    }
    private val exportUseCase = mockk<ExportMigrationDataUseCase>()
    private val resolvePathUseCase = mockk<ResolveMigrationFilePathUseCase>()
    private var session: MigrationSession? = MigrationSession(code = CODE, generateToken = { TOKEN })

    private val audioFile = MigrationFile(MigrationFileKind.AUDIO, "a.m4a")

    private fun serverTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val audioPath = tempFolder.newFile("a.m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }.absolutePath
        coEvery { exportUseCase() } returns Result.success(
            MigrationExport(
                snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                files = listOf(MigrationFileEntry(audioFile, size = 3)),
                downloadSettings = null,
            ),
        )
        every { resolvePathUseCase(audioFile) } returns Result.success(audioPath)
        every { resolvePathUseCase(MigrationFile(MigrationFileKind.AUDIO, "secret.db")) } returns
            Result.success(tempFolder.newFile("secret.db").absolutePath)

        application {
            fileShareModule(
                importUploadedAudioMedia = mockk(),
                validateUploadedAudioFileName = ValidateUploadedAudioFileNameUseCase(),
                updateAudioMedia = mockk(),
                saveAudioMediaImage = mockk(),
                uploadCacheDir = tempFolder.root,
                onUploadImported = {},
                currentMigrationSession = { session },
                cleanupAnomalousData = cleanupUseCase,
                exportMigrationData = exportUseCase,
                resolveMigrationFilePath = resolvePathUseCase,
            )
        }
        block()
    }

    private suspend fun ApplicationTestBuilder.pair(code: String): HttpResponse =
        client.post(FileShareConstants.MIGRATION_PAIR_PATH) {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"$code","deviceName":"iPhone"}""")
        }

    private suspend fun ApplicationTestBuilder.manifest(token: String?): HttpResponse =
        client.get(FileShareConstants.MIGRATION_MANIFEST_PATH) { token?.let { bearerAuth(it) } }

    private suspend fun ApplicationTestBuilder.file(kind: String, name: String, token: String?): HttpResponse =
        client.get("${FileShareConstants.MIGRATION_FILES_PATH}/$kind/$name") { token?.let { bearerAuth(it) } }

    @Test
    fun `이전 세션이 없으면 모든 경로가 404 다`() = serverTest {
        session = null

        assertEquals(HttpStatusCode.NotFound, pair(CODE).status)
        assertEquals(HttpStatusCode.NotFound, manifest(TOKEN).status)
        assertEquals(HttpStatusCode.NotFound, file("AUDIO", "a.m4a", TOKEN).status)
    }

    @Test
    fun `코드가 맞으면 토큰을 주고 틀리면 401 이다`() = serverTest {
        assertEquals(HttpStatusCode.Unauthorized, pair("000000").status)

        val response = pair(CODE)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(TOKEN))
    }

    @Test
    fun `코드를 정해진 횟수만큼 틀리면 맞는 코드도 401 이다`() = serverTest {
        repeat(MigrationSession.MAX_FAILED_ATTEMPTS) { pair("000000") }

        assertEquals(HttpStatusCode.Unauthorized, pair(CODE).status)
    }

    @Test
    fun `토큰이 없거나 틀리면 manifest 와 파일이 401 이다`() = serverTest {
        pair(CODE)

        assertEquals(HttpStatusCode.Unauthorized, manifest(null).status)
        assertEquals(HttpStatusCode.Unauthorized, manifest("wrong").status)
        assertEquals(HttpStatusCode.Unauthorized, file("AUDIO", "a.m4a", "wrong").status)
    }

    @Test
    fun `manifest 를 받은 뒤 거기 있는 파일만 내려준다`() = serverTest {
        pair(CODE)
        assertEquals(HttpStatusCode.NotFound, file("AUDIO", "a.m4a", TOKEN).status)

        val manifest = manifest(TOKEN)
        assertEquals(HttpStatusCode.OK, manifest.status)
        assertTrue(manifest.bodyAsText().contains(""""formatVersion":${FileShareConstants.MIGRATION_FORMAT_VERSION}"""))

        coVerifyOrder {
            cleanupUseCase()
            exportUseCase()
        }

        val response = file("AUDIO", "a.m4a", TOKEN)
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(response.bodyAsBytes()))
        assertEquals(HttpStatusCode.NotFound, file("AUDIO", "secret.db", TOKEN).status)
        assertEquals(HttpStatusCode.NotFound, file("IMAGE", "a.m4a", TOKEN).status)
    }

    @Test
    fun `목록에는 있지만 디스크에서 사라진 파일은 410 으로 세션 없음과 구분한다`() = serverTest {
        pair(CODE)
        manifest(TOKEN)
        java.io.File(tempFolder.root, "a.m4a").delete()

        assertEquals(HttpStatusCode.Gone, file("AUDIO", "a.m4a", TOKEN).status)
    }

    @Test
    fun `연결 전에는 데이터를 정리하지 않는다`() = serverTest {
        pair(CODE)

        coVerify(exactly = 0) { cleanupUseCase() }
    }

    @Test
    fun `정리에 실패하면 목록을 내려주지 않는다`() = serverTest {
        coEvery { cleanupUseCase() } returns Result.failure(IllegalStateException())
        pair(CODE)

        assertEquals(HttpStatusCode.ServiceUnavailable, manifest(TOKEN).status)
    }

    @Test
    fun `경로를 벗어나는 이름은 거부한다`() = serverTest {
        pair(CODE)
        manifest(TOKEN)

        assertEquals(HttpStatusCode.NotFound, file("AUDIO", "..%2F..%2Fa.m4a", TOKEN).status)
        assertEquals(HttpStatusCode.NotFound, file("AUDIO", "..%2Fsecret.db", TOKEN).status)
    }

    @Test
    fun `세션을 닫으면 발급한 토큰도 401 이다`() = serverTest {
        pair(CODE)
        manifest(TOKEN)

        session?.close()

        assertEquals(HttpStatusCode.Unauthorized, manifest(TOKEN).status)
        assertEquals(HttpStatusCode.Unauthorized, file("AUDIO", "a.m4a", TOKEN).status)
    }

    @Test
    fun `완료 알림은 토큰이 있어야 하고 세션에 결과를 남긴다`() = serverTest {
        var finished: MigrationFinishRequest? = null
        session = MigrationSession(code = CODE, generateToken = { TOKEN }, onFinished = { finished = it })
        pair(CODE)

        val unauthorized = client.post(FileShareConstants.MIGRATION_FINISH_PATH) {
            contentType(ContentType.Application.Json)
            setBody("""{"newAudioMediaCount":3,"failedCount":1}""")
        }
        val response = client.post(FileShareConstants.MIGRATION_FINISH_PATH) {
            bearerAuth(TOKEN)
            contentType(ContentType.Application.Json)
            setBody("""{"newAudioMediaCount":3,"failedCount":1}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(MigrationFinishRequest(newAudioMediaCount = 3, failedCount = 1), finished)
    }

    private companion object {
        const val CODE = "123456"
        const val TOKEN = "issued-token"
    }
}
