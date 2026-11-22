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

class MainViewModelUploadTest {

    private val dir = createTempDirectory("upload-test").toFile()
    private val commits: MutableList<List<String>> = Collections.synchronizedList(mutableListOf())

    /** 파일 이름으로 받기 응답을 정한다. 기본은 바로 받기 성공이다. */
    private var onStage: suspend (fileName: String) -> UploadStageResponse = { name ->
        UploadStageResponse(success = true, message = "", stageId = name)
    }

    private val viewModel = MainViewModel(
        discover = { DiscoveredDevice("phone", 1) },
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

    private suspend fun drop(vararg names: String) {
        viewModel.uiState.first { it.discoveryState == DiscoveryState.FOUND }
        viewModel.onAudioFilesDrop(names.map { File(dir, it).apply { writeBytes(byteArrayOf(1)) } })
    }

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
