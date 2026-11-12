package com.kintmin.platform.service

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.platform.service.migration.MigrationSession
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 기존 PC → Android 엔드포인트의 동작. 토큰 없이 받아야 한다(배포된 Desktop 앱이 토큰을 모른다). 단, 이전 세션 동안은 거절한다. */
class FileShareServerModuleTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val importUseCase = mockk<ImportUploadedAudioMediaUseCase>()
    private val updateUseCase = mockk<UpdateAudioMediaUseCase>()
    private val saveImageUseCase = mockk<SaveAudioMediaImageUseCase>()
    private val imported = mutableListOf<ImportedAudioMediaResult>()

    private fun serverTest(
        migrationSession: MigrationSession? = null,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application {
            fileShareModule(
                importUploadedAudioMedia = importUseCase,
                updateAudioMedia = updateUseCase,
                saveAudioMediaImage = saveImageUseCase,
                uploadCacheDir = tempFolder.root,
                onUploadImported = { imported += it },
                currentMigrationSession = { migrationSession },
                cleanupAnomalousData = mockk(),
                exportMigrationData = mockk(),
                resolveMigrationFilePath = mockk(),
            )
        }
        block()
    }

    @Test
    fun `업로드는 본문을 임시 파일로 받아 가져오고 끝나면 임시 파일을 지운다`() = serverTest {
        var receivedBytes: ByteArray? = null
        var receivedName: String? = null
        coEvery { importUseCase(any(), any()) } answers {
            receivedBytes = File(firstArg<String>()).readBytes()
            receivedName = secondArg()
            Result.success(ImportedAudioMediaResult(audioMedia(id = 7, name = "곡"), playlistIdOnDownload = 3))
        }

        val response = client.post(FileShareConstants.HTTP_UPLOAD_PATH) {
            header(FileShareConstants.HEADER_FILE_NAME, "song.m4a")
            setBody(byteArrayOf(1, 2, 3))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.getValue("success").jsonPrimitive.boolean)
        assertEquals(7, body.getValue("audioMediaId").jsonPrimitive.int)
        assertEquals("song.m4a", receivedName)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(receivedBytes))
        assertEquals(listOf(7), imported.map { it.audioMedia.id })
        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `이미 있는 음원 업로드는 422 와 중복 안내를 돌려준다`() = serverTest {
        coEvery { importUseCase(any(), any()) } returns Result.failure(AlreadyDownloadedMedia())

        val response = client.post(FileShareConstants.HTTP_UPLOAD_PATH) { setBody(byteArrayOf(1)) }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.bodyAsText().contains("이미 저장된 파일입니다."))
        assertTrue(imported.isEmpty())
    }

    @Test
    fun `아티스트 일괄 변경은 모든 id 에 적용한다`() = serverTest {
        coEvery { updateUseCase(id = any(), artist = any()) } returns Result.success(Unit)

        val response = client.post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
            contentType(ContentType.Application.Json)
            setBody("""{"audioMediaIds":[1,2],"artist":"가수"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { updateUseCase(id = 1, artist = "가수") }
        coVerify { updateUseCase(id = 2, artist = "가수") }
    }

    @Test
    fun `아티스트가 비어 있으면 400 이다`() = serverTest {
        val response = client.post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
            contentType(ContentType.Application.Json)
            setBody("""{"audioMediaIds":[1],"artist":" "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `썸네일 일괄 변경은 id 마다 이미지를 저장해 연결한다`() = serverTest {
        coEvery { saveImageUseCase(any()) } returns Result.success("/img/a.jpg")
        coEvery { updateUseCase(id = any(), imageFileFullPath = any()) } returns Result.success(Unit)

        val response = client.post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) {
            header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS, "4, 5")
            setBody(byteArrayOf(9))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify(exactly = 2) { saveImageUseCase(any()) }
        coVerify { updateUseCase(id = 4, imageFileFullPath = "/img/a.jpg") }
        coVerify { updateUseCase(id = 5, imageFileFullPath = "/img/a.jpg") }
    }

    @Test
    fun `썸네일 대상 id 가 없으면 400 이다`() = serverTest {
        val response = client.post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) { setBody(byteArrayOf(9)) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `이전 세션이 열려 있으면 업로드와 일괄 변경을 409 로 거절한다`() = serverTest(migrationSession = MigrationSession()) {
        val responses = listOf(
            client.post(FileShareConstants.HTTP_UPLOAD_PATH) { setBody(byteArrayOf(1)) },
            client.post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
                contentType(ContentType.Application.Json)
                setBody("""{"audioMediaIds":[1],"artist":"가수"}""")
            },
            client.post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) {
                header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS, "1")
                setBody(byteArrayOf(9))
            },
        )

        responses.forEach { response ->
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertTrue(response.bodyAsText().contains("다른 기기와 데이터 이전 중입니다."))
        }
        coVerify(exactly = 0) { importUseCase(any(), any()) }
        coVerify(exactly = 0) { updateUseCase(id = any(), artist = any()) }
        coVerify(exactly = 0) { saveImageUseCase(any()) }
    }

    private fun audioMedia(id: Int, name: String) = AudioMedia(
        id = id,
        source = "s",
        name = name,
        artist = "a",
        description = "",
        audioDuration = null,
        audioFileFullPath = "/a.m4a",
        imageFileFullPath = null,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )
}
