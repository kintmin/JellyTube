package com.kintmin.data.file_share

import com.kintmin.data.file_share.migration.MigrationSession
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.domain.file_share.usecase.CreatePcUploadHandlerUseCase
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.UploadCommitRequest
import com.kintmin.log.AppLog
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileShareServerModuleTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val importUseCase = mockk<ImportUploadedAudioMediaUseCase>()
    private val updateUseCase = mockk<UpdateAudioMediaUseCase>()
    private val saveImageUseCase = mockk<SaveAudioMediaImageUseCase>()
    private val imported = mutableListOf<ImportedAudioMediaResult>()
    private val routeAppLog = mockk<AppLog>(relaxed = true)

    private fun serverTest(
        migrationSession: MigrationSession? = null,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        application { testFileShareModule(migrationSession) }
        block()
    }

    private fun Application.testFileShareModule(migrationSession: MigrationSession?) = fileShareModule(
        pcUploadHandler = CreatePcUploadHandlerUseCase(
            importUploadedAudioMedia = importUseCase,
            validateUploadedAudioFileName = ValidateUploadedAudioFileNameUseCase(),
            updateAudioMedia = updateUseCase,
            saveAudioMediaImage = saveImageUseCase,
            playlistRepository = mockk { coEvery { getPlaylist(any()) } returns Result.failure(Exception()) },
            audioTrackRepository = mockk { every { getSecretAudioMediaIdListFlow() } returns flowOf(emptyList()) },
            appLog = mockk(relaxed = true),
        ).invoke { imported += it },
        appLog = routeAppLog,
        uploadTempDir = tempFolder.root.path,
        receiveKeepAlive = KeepAliveByForegroundService,
        currentMigrationSession = { migrationSession },
        migrationSendHandler = mockk(),
    )

    private suspend fun ApplicationTestBuilder.stage(fileName: String, bytes: ByteArray) =
        client.post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) {
            header(FileShareConstants.HEADER_FILE_NAME, fileName)
            setBody(bytes)
        }

    private suspend fun ApplicationTestBuilder.commit(vararg stageIds: String) =
        client.post(FileShareConstants.HTTP_UPLOAD_COMMIT_PATH) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UploadCommitRequest(stageIds.toList())))
        }

    private suspend fun stageId(response: HttpResponse) =
        Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("stageId").jsonPrimitive.content

    private suspend fun commitResults(response: HttpResponse) =
        Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("results").jsonArray.map { it.jsonObject }

    @Test
    fun `stage 는 본문을 임시 파일로 받아 두기만 하고 목록에는 넣지 않는다`() = serverTest {
        val response = stage("song.m4a", byteArrayOf(1, 2, 3))

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.getValue("success").jsonPrimitive.boolean)
        assertTrue(body.getValue("stageId").jsonPrimitive.content.isNotEmpty())
        coVerify(exactly = 0) { importUseCase(any()) }
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(tempFolder.root.listFiles()!!.single().readBytes()))
    }

    @Test
    fun `commit 은 요청한 순서대로 넣고 임시 파일을 지운다`() = serverTest {
        val received = slot<List<UploadedAudioFile>>()
        var receivedBytes: List<ByteArray> = emptyList()
        coEvery { importUseCase(capture(received)) } answers {
            val uploads = firstArg<List<UploadedAudioFile>>()
            receivedBytes = uploads.map { File(it.fileFullPath).readBytes() }
            uploads.indices.map { index ->
                Result.success(ImportedAudioMediaResult(audioMedia(id = index + 10, name = "곡$index"), playlistIdOnDownload = 3))
            }
        }
        val b = stageId(stage("b.mp3", byteArrayOf(2)))
        val a = stageId(stage("a.mp3", byteArrayOf(1)))

        val response = commit(a, b)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("a.mp3", "b.mp3"), received.captured.map { it.originalFileName })
        assertEquals(listOf<Byte>(1, 2), receivedBytes.map { it.single() })
        assertEquals(listOf(10, 11), commitResults(response).map { it.getValue("audioMediaId").jsonPrimitive.int })
        assertEquals(listOf(10, 11), imported.map { it.audioMedia.id })
        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `commit 에서 모르는 id 와 실패한 곡은 그 자리만 실패다`() = serverTest {
        coEvery { importUseCase(any()) } returns listOf(Result.failure(AlreadyDownloadedMedia()))
        val known = stageId(stage("song.mp3", byteArrayOf(1)))

        val results = commitResults(commit("unknown", known))

        assertEquals(listOf(false, false), results.map { it.getValue("success").jsonPrimitive.boolean })
        assertEquals("만료된 업로드입니다. 다시 보내 주세요.", results[0].getValue("message").jsonPrimitive.content)
        assertEquals("이미 저장된 파일입니다.", results[1].getValue("message").jsonPrimitive.content)
        coVerify { importUseCase(match { it.size == 1 }) }
        assertTrue(imported.isEmpty())
    }

    @Test
    fun `같은 stageId 는 두 번 반영하지 않는다`() = serverTest {
        coEvery { importUseCase(any()) } answers {
            firstArg<List<UploadedAudioFile>>().map { Result.failure(AlreadyDownloadedMedia()) }
        }
        val id = stageId(stage("song.mp3", byteArrayOf(1)))

        commit(id)
        val second = commitResults(commit(id)).single()

        assertEquals("만료된 업로드입니다. 다시 보내 주세요.", second.getValue("message").jsonPrimitive.content)
    }

    @Test
    fun `반영하지 않은 채 서버가 멈추면 받아 둔 임시 파일을 지운다`() {
        testApplication {
            application { testFileShareModule(migrationSession = null) }
            stage("song.mp3", byteArrayOf(1))
            assertEquals(1, tempFolder.root.listFiles()!!.size)
        }

        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `파일 이름이 없거나 확장자가 없으면 받지 않고 400 이다`() = serverTest {
        val missing = client.post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) { setBody(byteArrayOf(1)) }
        val noExtension = stage("song", byteArrayOf(1))

        assertEquals(HttpStatusCode.BadRequest, missing.status)
        assertEquals(HttpStatusCode.BadRequest, noExtension.status)
        assertTrue(noExtension.bodyAsText().contains("확장자가 없는 파일은 저장할 수 없습니다."))
        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `받기를 거절하면 사유와 확장자와 크기를 로그로 남긴다`() = serverTest {
        stage("song.mp3", ByteArray(0))

        verify {
            routeAppLog.sendDebugLog(
                match {
                    it.message == "받기 거절: 빈 파일은 저장할 수 없습니다. ext=mp3 length=0 received=0"
                },
            )
        }
    }

    @Test
    fun `빈 본문은 받아 두지 않고 400 이다`() = serverTest {
        val response = stage("song.mp3", ByteArray(0))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("빈 파일은 저장할 수 없습니다."))
        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
    }

    @Test
    fun `알린 길이보다 덜 받으면 받아 두지 않고 400 이다`() = serverTest {
        val response = client.post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) {
            header(FileShareConstants.HEADER_FILE_NAME, "song.mp3")
            setBody(
                object : OutgoingContent.ReadChannelContent() {
                    override val contentLength: Long = 10
                    override fun readFrom(): ByteReadChannel = ByteReadChannel(byteArrayOf(1, 2, 3))
                },
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("전송이 중간에 끊겼습니다."))
        assertTrue(tempFolder.root.listFiles().isNullOrEmpty())
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
            client.post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) { setBody(byteArrayOf(1)) },
            client.post(FileShareConstants.HTTP_UPLOAD_COMMIT_PATH) {
                contentType(ContentType.Application.Json)
                setBody("""{"stageIds":["x"]}""")
            },
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
        coVerify(exactly = 0) { importUseCase(any()) }
        coVerify(exactly = 0) { updateUseCase(id = any(), artist = any()) }
        coVerify(exactly = 0) { saveImageUseCase(any()) }
    }

    @Test
    fun `거절할 때도 본문을 끝까지 읽어 보내던 클라이언트가 응답을 받게 한다`() = serverTest(migrationSession = MigrationSession()) {
        val chunk = ByteArray(64 * 1024)
        var writtenBytes = 0L
        val body = object : OutgoingContent.WriteChannelContent() {
            override val contentLength = chunk.size * LARGE_BODY_CHUNKS.toLong()
            override suspend fun writeTo(channel: ByteWriteChannel) {
                repeat(LARGE_BODY_CHUNKS) {
                    channel.writeFully(chunk)
                    channel.flush()
                    writtenBytes += chunk.size
                }
            }
        }

        val conflict = client.post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) {
            header(FileShareConstants.HEADER_FILE_NAME, "song.m4a")
            setBody(body)
        }

        assertEquals(HttpStatusCode.Conflict, conflict.status)
        assertEquals(body.contentLength, writtenBytes)
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

private const val LARGE_BODY_CHUNKS = 128
