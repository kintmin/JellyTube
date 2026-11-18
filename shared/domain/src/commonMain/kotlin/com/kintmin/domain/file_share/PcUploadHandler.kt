package com.kintmin.domain.file_share

import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult

/** PC 가 보낸 요청의 처리. 받는 서버는 전송만 맡고 처리는 여기로 넘긴다. */
interface PcUploadHandler {
    fun validateFileName(rawFileName: String?): Result<String>

    suspend fun importUploadedAudio(uploadedFileFullPath: String, originalFileName: String): Result<ImportedAudioMediaResult>

    suspend fun applyArtist(audioMediaIds: List<Int>, artist: String): Result<Unit>

    suspend fun applyThumbnail(audioMediaIds: List<Int>, imageData: ByteArray): Result<Unit>
}
