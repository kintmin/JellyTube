package com.kintmin.domain.file_share.usecase

import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.model.UploadedAudioFile
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.domain.file_share.PcUploadHandler
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog
import com.kintmin.log.secret

class CreatePcUploadHandlerUseCase(
    private val importUploadedAudioMedia: ImportUploadedAudioMediaUseCase,
    private val validateUploadedAudioFileName: ValidateUploadedAudioFileNameUseCase,
    private val updateAudioMedia: UpdateAudioMediaUseCase,
    private val saveAudioMediaImage: SaveAudioMediaImageUseCase,
    private val appLog: AppLog,
) {
    operator fun invoke(onUploadImported: (ImportedAudioMediaResult) -> Unit): PcUploadHandler = object : PcUploadHandler {
        override fun validateFileName(rawFileName: String?): Result<String> = validateUploadedAudioFileName(rawFileName)

        override suspend fun importUploadedAudios(uploads: List<UploadedAudioFile>) =
            importUploadedAudioMedia(uploads).also { results ->
                uploads.zip(results).forEach { (upload, result) ->
                    result
                        .onSuccess { log("들여옴: id=${it.audioMedia.id} name=${secret(it.audioMedia.name)}") }
                        .onSuccess(onUploadImported)
                        .onFailure {
                            if (it is AlreadyDownloadedMedia) log("이미 있는 파일이라 들여오지 않음: ${upload.originalFileName}")
                            else logError("들여오기 실패: ${upload.originalFileName}", it)
                        }
                }
            }

        override suspend fun applyArtist(audioMediaIds: List<Int>, artist: String): Result<Unit> = runCatching {
            audioMediaIds.forEach { id -> updateAudioMedia(id = id, artist = artist).getOrThrow() }
        }
            .onSuccess { log("아티스트 적용: ${audioMediaIds.size}곡 → $artist") }
            .onFailure { logError("아티스트 적용 실패: ${audioMediaIds.size}곡", it) }

        override suspend fun applyThumbnail(audioMediaIds: List<Int>, imageData: ByteArray): Result<Unit> = runCatching {
            audioMediaIds.forEach { id ->
                val imageFileFullPath = saveAudioMediaImage(imageData).getOrThrow()
                updateAudioMedia(id = id, imageFileFullPath = imageFileFullPath).getOrThrow()
            }
        }
            .onSuccess { log("썸네일 적용: ${audioMediaIds.size}곡") }
            .onFailure { logError("썸네일 적용 실패: ${audioMediaIds.size}곡", it) }
    }

    private fun log(message: String) = appLog.sendDebugLog(DebugLog(LOG_TAG, message))

    private fun logError(message: String, error: Throwable) = appLog.sendErrorLog(ErrorLog(LOG_TAG, message, error))
}

private const val LOG_TAG = "PcUpload"
