package com.kintmin.desktop.upload

import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import java.io.File

class FileUploader(
    private val hostAddress: String,
    private val port: Int,
) {
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 0
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun uploadFile(file: File): Result<UploadResponse> = runCatching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_UPLOAD_PATH}") {
            header(FileShareConstants.HEADER_FILE_NAME, file.name)
            // 긴 음원(1시간 이상)을 통째로 메모리에 올리지 않고 스트리밍으로 전송한다.
            setBody(StreamingFileContent(file))
        }
        response.body<UploadResponse>()
    }

    /** 파일을 메모리에 적재하지 않고 Content-Length를 명시해 스트리밍 전송하는 본문. */
    private class StreamingFileContent(private val file: File) : OutgoingContent.ReadChannelContent() {
        override val contentType: ContentType = ContentType.Application.OctetStream
        override val contentLength: Long = file.length()
        override fun readFrom(): ByteReadChannel = file.readChannel()
    }

    suspend fun updateArtist(audioMediaIds: List<Int>, artist: String): Result<FileShareResponse> = runCatching {
        val response = client.post("http://$hostAddress:$port${FileShareConstants.HTTP_BULK_ARTIST_PATH}") {
            contentType(ContentType.Application.Json)
            setBody(BulkArtistUpdateRequest(audioMediaIds = audioMediaIds, artist = artist))
        }
        response.body<FileShareResponse>()
    }

    suspend fun updateThumbnail(audioMediaIds: List<Int>, imageFile: File): Result<FileShareResponse> = runCatching {
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
