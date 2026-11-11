package com.kintmin.platform.service

import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import com.kintmin.domain.data_migration.usecase.ResolveMigrationFilePathUseCase
import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadResponse
import com.kintmin.platform.service.migration.MigrationSession
import com.kintmin.platform.service.migration.migrationRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

internal fun Application.fileShareModule(
    importUploadedAudioMedia: ImportUploadedAudioMediaUseCase,
    updateAudioMedia: UpdateAudioMediaUseCase,
    saveAudioMediaImage: SaveAudioMediaImageUseCase,
    uploadCacheDir: File,
    onUploadImported: (ImportedAudioMediaResult) -> Unit,
    currentMigrationSession: () -> MigrationSession?,
    exportMigrationData: ExportMigrationDataUseCase,
    resolveMigrationFilePath: ResolveMigrationFilePathUseCase,
) {
    install(WebSockets)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    routing {
        migrationRoutes(currentMigrationSession, exportMigrationData, resolveMigrationFilePath)

        webSocket(FileShareConstants.WS_STATUS_PATH) {
            send("running")
            for (frame in incoming) {
                if (frame is Frame.Close) break
            }
        }

        /**
         * POST /upload
         * Header: X-File-Name: <original file name with extension>
         * Body: raw audio bytes
         */
        post(FileShareConstants.HTTP_UPLOAD_PATH) {
            val originalFileName = call.request.header(FileShareConstants.HEADER_FILE_NAME)
                ?: "upload.mp3"

            // 대용량 WAV도 힙에 통째로 올리지 않도록 임시 파일로 스트리밍 수신한다.
            val tempFile = File.createTempFile(TEMP_UPLOAD_PREFIX, null, uploadCacheDir)
            try {
                runCatching {
                    call.receiveChannel().toInputStream().use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }.getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        UploadResponse(success = false, message = "파일을 읽을 수 없습니다."),
                    )
                    return@post
                }

                importUploadedAudioMedia(tempFile.absolutePath, originalFileName)
                    .onSuccess { result ->
                        onUploadImported(result)
                        call.respond(
                            HttpStatusCode.OK,
                            UploadResponse(
                                success = true,
                                message = "업로드 성공",
                                audioMediaId = result.audioMedia.id,
                                title = result.audioMedia.name,
                            ),
                        )
                    }
                    .onFailure { error ->
                        val message = when (error) {
                            is AlreadyDownloadedMedia -> "이미 저장된 파일입니다."
                            else -> error.message ?: "업로드 실패"
                        }
                        call.respond(
                            HttpStatusCode.UnprocessableEntity,
                            UploadResponse(success = false, message = message),
                        )
                    }
            } finally {
                tempFile.delete()
            }
        }

        post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
            val request = runCatching { call.receive<BulkArtistUpdateRequest>() }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FileShareResponse(success = false, message = "요청을 읽을 수 없습니다."),
                    )
                    return@post
                }
            if (request.audioMediaIds.isEmpty() || request.artist.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FileShareResponse(success = false, message = "적용할 음원이나 아티스트가 없습니다."),
                )
                return@post
            }

            val result = runCatching {
                request.audioMediaIds.forEach { id ->
                    updateAudioMedia(id = id, artist = request.artist).getOrThrow()
                }
            }
            call.respond(
                if (result.isSuccess) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                FileShareResponse(
                    success = result.isSuccess,
                    message = result.exceptionOrNull()?.message ?: "아티스트 적용 완료",
                ),
            )
        }

        post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) {
            val ids = call.request.header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS)
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                .orEmpty()
            if (ids.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FileShareResponse(success = false, message = "적용할 음원이 없습니다."),
                )
                return@post
            }

            val bytes = runCatching {
                call.receiveChannel().toInputStream().readBytes()
            }.getOrElse {
                call.respond(
                    HttpStatusCode.BadRequest,
                    FileShareResponse(success = false, message = "이미지를 읽을 수 없습니다."),
                )
                return@post
            }
            val result = runCatching {
                ids.forEach { id ->
                    val imageFileFullPath = saveAudioMediaImage(bytes).getOrThrow()
                    updateAudioMedia(id = id, imageFileFullPath = imageFileFullPath).getOrThrow()
                }
            }
            call.respond(
                if (result.isSuccess) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                FileShareResponse(
                    success = result.isSuccess,
                    message = result.exceptionOrNull()?.message ?: "썸네일 적용 완료",
                ),
            )
        }
    }
}

private const val TEMP_UPLOAD_PREFIX = "upload_"
