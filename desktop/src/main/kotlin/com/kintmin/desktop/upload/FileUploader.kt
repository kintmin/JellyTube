package com.kintmin.desktop.upload

import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadCommitRequest
import com.kintmin.fileshare.UploadCommitResponse
import com.kintmin.fileshare.UploadStageResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.File

class FileUploader(
    private val hostAddress: String,
    private val port: Int,
    engine: HttpClientEngine = CIO.create { requestTimeout = 0 },
) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /** 받는 기기가 임시로 받아 두기만 한다. 목록에는 [commit] 이 넣는다. [onProgress] 는 0..1 이다. */
    suspend fun stage(file: File, onProgress: (Float) -> Unit): Result<UploadStageResponse> = catching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_UPLOAD_STAGE_PATH}") {
            header(FileShareConstants.HEADER_FILE_NAME, file.name)
            setBody(FileContent(file))
            onUpload { sent, total -> if (total != null && total > 0) onProgress(sent.toFloat() / total) }
        }
        response.body<UploadStageResponse>()
    }

    /** 결과는 [stageIds] 와 같은 순서다. */
    suspend fun commit(stageIds: List<String>): Result<UploadCommitResponse> = catching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_UPLOAD_COMMIT_PATH}") {
            contentType(ContentType.Application.Json)
            setBody(UploadCommitRequest(stageIds))
        }
        response.body<UploadCommitResponse>()
    }

    /** 길이를 알려야 받는 쪽이 중간에 끊긴 전송을 가려낸다. 채널만 넘기면 chunked 로 간다. */
    private class FileContent(private val file: File) : OutgoingContent.ReadChannelContent() {
        override val contentType: ContentType = ContentType.Application.OctetStream
        override val contentLength: Long = file.length()
        override fun readFrom(): ByteReadChannel = file.readChannel()
    }

    suspend fun updateArtist(audioMediaIds: List<Int>, artist: String): Result<FileShareResponse> = catching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_BULK_ARTIST_PATH}") {
            contentType(ContentType.Application.Json)
            setBody(BulkArtistUpdateRequest(audioMediaIds = audioMediaIds, artist = artist))
        }
        response.body<FileShareResponse>()
    }

    suspend fun updateThumbnail(audioMediaIds: List<Int>, imageFile: File): Result<FileShareResponse> = catching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_BULK_THUMBNAIL_PATH}") {
            header(FileShareConstants.HEADER_FILE_NAME, imageFile.name)
            header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS, audioMediaIds.joinToString(","))
            contentType(ContentType.Application.OctetStream)
            setBody(imageFile.readBytes())
        }
        response.body<FileShareResponse>()
    }

    fun close() {
        client.close()
    }
}

/** 항목을 지워 업로드를 취소했을 때 실패로 바뀌지 않게 취소는 그대로 던진다. */
private inline fun <T> catching(block: () -> T): Result<T> =
    runCatching(block).onFailure { if (it is CancellationException) throw it }
