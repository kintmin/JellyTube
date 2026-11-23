package com.kintmin.data.file_share

import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.common.suspendRunCatching
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.domain.file_share.ReceiveKeepAlive
import com.kintmin.fileshare.BulkArtistUpdateRequest
import com.kintmin.fileshare.FileShareConstants
import com.kintmin.fileshare.FileShareResponse
import com.kintmin.fileshare.UploadCommitRequest
import com.kintmin.fileshare.UploadCommitResponse
import com.kintmin.fileshare.UploadResponse
import com.kintmin.fileshare.UploadStageResponse
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.secret
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.contentLength
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.discard
import io.ktor.utils.io.readTo
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Application.installFileShareContentNegotiation() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

/** [installFileShareContentNegotiation] 이 설치돼 있어야 JSON 본문을 읽고 쓴다. */
@OptIn(ExperimentalUuidApi::class)
internal fun Route.pcUploadEndpoints(
    handler: PcUploadHandler,
    appLog: AppLog,
    uploadTempDir: String,
    receiveKeepAlive: ReceiveKeepAlive,
    stagedUploads: StagedUploads,
) {
    fun logRejected(reason: String, fileName: String?, contentLength: Long?, receivedBytes: Long? = null, error: Throwable? = null) {
        val ext = fileName?.substringAfterLast('.', missingDelimiterValue = "")
        appLog.sendDebugLog(
            DebugLog(
                LOG_TAG,
                "받기 거절: $reason name=${secret(fileName)} ext=$ext length=$contentLength received=$receivedBytes" +
                    (error?.let { " / $it" } ?: ""),
            ),
        )
    }

    post(FileShareConstants.HTTP_UPLOAD_STAGE_PATH) {
        val rawFileName = call.request.header(FileShareConstants.HEADER_FILE_NAME)
        val originalFileName = handler.validateFileName(rawFileName)
            .getOrElse { error ->
                logRejected(error.message.orEmpty(), rawFileName, call.request.contentLength())
                call.discardBody()
                call.respond(HttpStatusCode.BadRequest, UploadStageResponse(success = false, message = error.message.orEmpty()))
                return@post
            }

        receiveKeepAlive.keepAlive {
            // 대용량 WAV도 힙에 통째로 올리지 않도록 임시 파일로 스트리밍 수신한다.
            val stageId = Uuid.random().toString()
            val tempPath = Path(uploadTempDir, TEMP_UPLOAD_PREFIX + stageId)
            val contentLength = call.request.contentLength()
            suspend fun reject(reason: String, receivedBytes: Long?, error: Throwable? = null) {
                logRejected(reason, originalFileName, contentLength, receivedBytes, error)
                deleteTempFile(tempPath.toString())
                call.respond(HttpStatusCode.BadRequest, UploadStageResponse(success = false, message = reason))
            }

            val receivedBytes = suspendRunCatching {
                SystemFileSystem.sink(tempPath).use { sink -> call.receiveChannel().readTo(sink) }
            }.getOrElse { error ->
                reject("파일을 읽을 수 없습니다.", receivedBytes = null, error)
                return@keepAlive
            }
            val rejection = when {
                receivedBytes == 0L -> "빈 파일은 저장할 수 없습니다."
                contentLength?.let { it != receivedBytes } == true -> "전송이 중간에 끊겼습니다. 다시 보내 주세요."
                else -> null
            }
            if (rejection != null) {
                reject(rejection, receivedBytes)
                return@keepAlive
            }
            stagedUploads.put(stageId, UploadedAudioFile(tempPath.toString(), originalFileName, receivedBytes))
            call.respond(HttpStatusCode.OK, UploadStageResponse(success = true, message = "받기 완료", stageId = stageId))
        }
    }

    post(FileShareConstants.HTTP_UPLOAD_COMMIT_PATH) {
        val request = suspendRunCatching { call.receive<UploadCommitRequest>() }
            .getOrElse {
                call.respond(HttpStatusCode.BadRequest, FileShareResponse(success = false, message = "요청을 읽을 수 없습니다."))
                return@post
            }

        // 여러 곡을 넣다 서스펜드되면 일부만 들어간 채 PC 는 끊김만 받는다.
        receiveKeepAlive.keepAlive {
            val staged = request.stageIds.map(stagedUploads::take)
            val found = staged.filterNotNull()
            val imported = try {
                suspendRunCatching { handler.importUploadedAudios(found) }
                    .getOrElse { error -> found.map { Result.failure(error) } }
            } finally {
                found.forEach { deleteTempFile(it.fileFullPath) }
            }
            val importedIterator = imported.iterator()
            val results = staged.map { upload ->
                if (upload == null) {
                    appLog.sendDebugLog(DebugLog(LOG_TAG, "들여오기 거절: 만료된 업로드"))
                    UploadResponse(success = false, message = "만료된 업로드입니다. 다시 보내 주세요.")
                } else {
                    importedIterator.next().toUploadResponse()
                }
            }
            call.respond(HttpStatusCode.OK, UploadCommitResponse(results))
        }
    }

    post(FileShareConstants.HTTP_BULK_ARTIST_PATH) {
        val request = suspendRunCatching { call.receive<BulkArtistUpdateRequest>() }
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

        // 여러 곡에 적용하다 서스펜드되면 일부에만 적용된 채 PC 는 끊김만 받는다.
        receiveKeepAlive.keepAlive {
            val result = handler.applyArtist(request.audioMediaIds, request.artist)
            call.respond(
                if (result.isSuccess) HttpStatusCode.OK else HttpStatusCode.UnprocessableEntity,
                FileShareResponse(
                    success = result.isSuccess,
                    message = result.exceptionOrNull()?.message ?: "아티스트 적용 완료",
                ),
            )
        }
    }

    post(FileShareConstants.HTTP_BULK_THUMBNAIL_PATH) {
        val ids = call.request.header(FileShareConstants.HEADER_AUDIO_MEDIA_IDS)
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            .orEmpty()
        if (ids.isEmpty()) {
            call.discardBody()
            call.respond(
                HttpStatusCode.BadRequest,
                FileShareResponse(success = false, message = "적용할 음원이 없습니다."),
            )
            return@post
        }

        receiveKeepAlive.keepAlive {
            val bytes = suspendRunCatching { call.receive<ByteArray>() }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        FileShareResponse(success = false, message = "이미지를 읽을 수 없습니다."),
                    )
                    return@keepAlive
                }
            val result = handler.applyThumbnail(ids, bytes)
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

/**
 * 본문을 읽지 않고 거절하면 서버가 연결을 닫는다. 아직 보내던 클라이언트는 응답 대신 끊김을 받아
 * 거절 사유를 보여 주지 못한다.
 */
suspend fun ApplicationCall.discardBody() {
    suspendRunCatching { receiveChannel().discard() }
}

private fun Result<ImportedAudioMediaResult>.toUploadResponse(): UploadResponse = fold(
    onSuccess = { result ->
        UploadResponse(
            success = true,
            message = "업로드 성공",
            audioMediaId = result.audioMedia.id,
            title = result.audioMedia.name,
        )
    },
    onFailure = { error ->
        val message = when (error) {
            is AlreadyDownloadedMedia -> "이미 저장된 파일입니다."
            else -> error.message ?: "업로드 실패"
        }
        UploadResponse(success = false, message = message)
    },
)

private const val TEMP_UPLOAD_PREFIX = "upload_"
private const val LOG_TAG = "PcUpload"
