package com.kintmin.desktop

import com.kintmin.desktop.discovery.DiscoveredDevice
import com.kintmin.desktop.upload.FileUploader
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.UploadCommitRequest
import com.kintmin.fileshare.UploadCommitResponse
import com.kintmin.fileshare.UploadResponse
import com.kintmin.fileshare.UploadStageResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainViewModelTest {

    private val dir = createTempDirectory("upload-test").toFile()
    private val commits: MutableList<List<String>> = Collections.synchronizedList(mutableListOf())

    /** 파일 이름으로 받기 응답을 정한다. 기본은 바로 받기 성공이다. */
    private var onStage: suspend (fileName: String) -> UploadStageResponse = { name ->
        UploadStageResponse(success = true, message = "", stageId = name)
    }

    private var discoverCalls = 0

    private val viewModel = MainViewModel(
        discover = {
            discoverCalls++
            DiscoveredDevice("phone", 1)
        },
        createUploader = { FileUploader(it.hostAddress, it.port, MockEngine { request -> handle(request) }) },
    )

    @AfterTest
    fun tearDown() {
        viewModel.onDispose()
        dir.deleteRecursively()
    }

    @Test
    fun `늦게 받아진 곡이 있어도 반영은 이름순이다`() = runTest {
        onStage = { name ->
            delay(if (name == "2.mp3") 300 else 0)
            UploadStageResponse(success = true, message = "", stageId = name)
        }

        drop("b.mp3", "10.mp3", "2.mp3", "A.mp3")
        val state = awaitAllDone()

        assertEquals(listOf(listOf("2.mp3", "10.mp3", "A.mp3", "b.mp3")), commits)
        assertEquals(listOf("2.mp3", "10.mp3", "A.mp3", "b.mp3"), state.uploadItems.map { it.file.name })
        assertEquals(List(4) { UploadStatus.SUCCESS }, state.uploadItems.map { it.status })
    }

    @Test
    fun `뒤 묶음이 먼저 받아져도 앞 묶음 뒤에 반영한다`() = runTest {
        onStage = { name ->
            delay(if (name == "slow.mp3") 300 else 0)
            UploadStageResponse(success = true, message = "", stageId = name)
        }

        drop("slow.mp3")
        drop("fast.mp3")
        awaitAllDone()

        assertEquals(listOf(listOf("slow.mp3"), listOf("fast.mp3")), commits)
    }

    @Test
    fun `받기에 실패한 곡은 반영에서 빠지고 이유를 남긴다`() = runTest {
        onStage = { name ->
            if (name == "bad.mp3") {
                UploadStageResponse(success = false, message = "빈 파일은 저장할 수 없습니다.")
            } else {
                UploadStageResponse(success = true, message = "", stageId = name)
            }
        }

        drop("a.mp3", "bad.mp3")
        val state = awaitAllDone()

        assertEquals(listOf(listOf("a.mp3")), commits)
        val bad = state.uploadItems.single { it.file.name == "bad.mp3" }
        assertEquals(UploadStatus.FAILURE, bad.status)
        assertEquals("빈 파일은 저장할 수 없습니다.", bad.errorMessage)
    }

    @Test
    fun `보내는 중에 지운 곡은 취소되고 반영하지 않는다`() = runTest {
        onStage = { name ->
            delay(if (name == "long.mp3") 60_000 else 0)
            UploadStageResponse(success = true, message = "", stageId = name)
        }

        drop("a.mp3", "long.mp3")
        val long = viewModel.uiState.first { s -> s.uploadItems.any { it.file.name == "long.mp3" && it.status == UploadStatus.UPLOADING } }
            .uploadItems.single { it.file.name == "long.mp3" }
        viewModel.onRemoveItem(long.id)
        val state = awaitAllDone()

        assertEquals(listOf(listOf("a.mp3")), commits)
        assertEquals(listOf("a.mp3"), state.uploadItems.map { it.file.name })
    }

    @Test
    fun `기기가 없을 때 드롭하면 상태 카드에 이유를 보여 주고 목록에 넣지 않는다`() = runTest {
        val offline = MainViewModel(discover = { null })
        try {
            offline.uiState.first { it.discoveryState == DiscoveryState.NOT_FOUND }
            offline.onFilesDrop(files("a.mp3"))

            val state = offline.uiState.value
            assertTrue(state.uploadItems.isEmpty())
            assertEquals("기기가 연결되지 않아 업로드할 수 없습니다. 휴대폰에서 받기를 켠 뒤 새로고침해 주세요.", state.dropMessage)
        } finally {
            offline.onDispose()
        }
    }

    @Test
    fun `음원과 다른 파일을 섞어 놓으면 음원만 올리고 건너뛴 수를 알린다`() = runTest {
        drop("a.mp3", "notes.txt", "cover.png")
        val state = awaitAllDone()

        assertEquals(listOf("a.mp3"), state.uploadItems.map { it.file.name })
        assertEquals("음원이 아닌 파일 2개는 건너뛰었습니다.", state.dropMessage)
    }

    @Test
    fun `올릴 수 없는 파일만 놓으면 받을 수 있는 형식을 알린다`() = runTest {
        drop("notes.txt")

        assertTrue(viewModel.uiState.value.uploadItems.isEmpty())
        assertEquals("음원(mp3·flac 등)이나 썸네일 이미지(jpg·png·webp)만 놓을 수 있습니다.", viewModel.uiState.value.dropMessage)
    }

    @Test
    fun `이미지를 여러 장 놓으면 한 장만 된다고 알린다`() = runTest {
        drop("a.png", "b.jpg")

        assertEquals("썸네일 이미지는 1장만 놓을 수 있습니다.", viewModel.uiState.value.dropMessage)
    }

    @Test
    fun `업로드 완료된 곡이 없으면 썸네일을 적용하지 않고 이유를 알린다`() = runTest {
        drop("cover.png")

        assertEquals("업로드 완료된 음원이 없어 썸네일을 적용할 곳이 없습니다.", viewModel.uiState.value.bulkMessage)
    }

    @Test
    fun `업로드 중에는 기기를 다시 찾지 않는다`() = runTest {
        onStage = { name ->
            delay(60_000)
            UploadStageResponse(success = true, message = "", stageId = name)
        }
        drop("long.mp3")
        viewModel.uiState.first { s -> s.uploadItems.any { it.status == UploadStatus.UPLOADING } }

        viewModel.startDiscovery()

        assertEquals(DiscoveryState.FOUND, viewModel.uiState.value.discoveryState)
        assertEquals(1, discoverCalls)
    }

    @Test
    fun `실패 항목 다시 시도는 실패한 곡을 목록 순서대로 한 묶음으로 보낸다`() = runTest {
        var failing = true
        onStage = { name ->
            if (failing) UploadStageResponse(success = false, message = "실패") else UploadStageResponse(success = true, message = "", stageId = name)
        }
        drop("b.mp3", "a.mp3")
        awaitAllDone()

        failing = false
        viewModel.onRetryAllFailed()
        val state = viewModel.uiState.first { s -> s.uploadItems.all { it.status == UploadStatus.SUCCESS } }

        assertEquals(listOf(listOf("a.mp3", "b.mp3")), commits)
        assertEquals(2, state.successCount)
    }

    @Test
    fun `업로드 중에는 목록을 비우지 않는다`() = runTest {
        onStage = { name ->
            delay(60_000)
            UploadStageResponse(success = true, message = "", stageId = name)
        }
        drop("long.mp3")
        viewModel.uiState.first { s -> s.uploadItems.any { it.status == UploadStatus.UPLOADING } }

        viewModel.onClearAll()

        assertEquals(1, viewModel.uiState.value.uploadItems.size)
    }

    @Test
    fun `일괄 적용은 업로드 완료된 곡이 있고 적용 중이 아닐 때만 할 수 있다`() {
        val done = UploadFileItem(file = File("a.mp3"), status = UploadStatus.SUCCESS, audioMediaId = 1)
        val ready = MainUiState(uploadItems = listOf(done), bulkArtist = "가수")

        assertTrue(ready.canApplyArtist)
        assertTrue(ready.canApplyThumbnail)
        assertFalse(ready.copy(bulkArtist = " ").canApplyArtist)
        assertFalse(ready.copy(isApplyingBulk = true).canApplyArtist)
        assertFalse(ready.copy(isApplyingBulk = true).canApplyThumbnail)
        assertFalse(ready.copy(uploadItems = listOf(done.copy(status = UploadStatus.FAILURE))).canApplyThumbnail)
    }

    private suspend fun drop(vararg names: String) {
        viewModel.uiState.first { it.discoveryState == DiscoveryState.FOUND }
        viewModel.onFilesDrop(files(*names))
    }

    private fun files(vararg names: String) = names.map { File(dir, it).apply { writeBytes(byteArrayOf(1)) } }

    private suspend fun awaitAllDone() = viewModel.uiState.first { state ->
        state.uploadItems.isNotEmpty() &&
            state.uploadItems.all { it.status == UploadStatus.SUCCESS || it.status == UploadStatus.FAILURE }
    }

    private suspend fun MockRequestHandleScope.handle(request: HttpRequestData) = when (request.url.encodedPath) {
        FileShareConstants.HTTP_UPLOAD_STAGE_PATH -> json(onStage(request.headers[FileShareConstants.HEADER_FILE_NAME]!!))
        FileShareConstants.HTTP_UPLOAD_COMMIT_PATH -> {
            val stageIds = Json.decodeFromString<UploadCommitRequest>(request.body.toByteArray().decodeToString()).stageIds
            commits += stageIds
            json(UploadCommitResponse(stageIds.mapIndexed { i, _ -> UploadResponse(success = true, message = "", audioMediaId = i) }))
        }
        else -> error("unexpected ${request.url}")
    }

    private inline fun <reified T> MockRequestHandleScope.json(body: T) = respond(
        content = Json.encodeToString(body),
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}
