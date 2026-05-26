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
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
            contentType(ContentType.Application.OctetStream)
            setBody(file.readBytes())
        }
        response.body<UploadResponse>()
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
