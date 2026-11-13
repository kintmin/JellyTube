package com.kintmin.domain.file_share.usecase

import com.kintmin.domain.audio_media.usecase.ImportUploadedAudioMediaUseCase
import com.kintmin.domain.audio_media.model.ImportedAudioMediaResult
import com.kintmin.domain.audio_media.usecase.SaveAudioMediaImageUseCase
import com.kintmin.domain.audio_media.usecase.UpdateAudioMediaUseCase
import com.kintmin.domain.audio_media.usecase.ValidateUploadedAudioFileNameUseCase
import com.kintmin.domain.file_share.PcUploadHandler

class CreatePcUploadHandlerUseCase(
    private val importUploadedAudioMedia: ImportUploadedAudioMediaUseCase,
    private val validateUploadedAudioFileName: ValidateUploadedAudioFileNameUseCase,
    private val updateAudioMedia: UpdateAudioMediaUseCase,
    private val saveAudioMediaImage: SaveAudioMediaImageUseCase,
) {
    operator fun invoke(onUploadImported: (ImportedAudioMediaResult) -> Unit): PcUploadHandler = object : PcUploadHandler {
        override fun validateFileName(rawFileName: String?): Result<String> = validateUploadedAudioFileName(rawFileName)

        override suspend fun importUploadedAudio(uploadedFileFullPath: String, originalFileName: String) =
            importUploadedAudioMedia(uploadedFileFullPath, originalFileName).onSuccess(onUploadImported)

        override suspend fun applyArtist(audioMediaIds: List<Int>, artist: String): Result<Unit> = runCatching {
            audioMediaIds.forEach { id -> updateAudioMedia(id = id, artist = artist).getOrThrow() }
        }

        override suspend fun applyThumbnail(audioMediaIds: List<Int>, imageData: ByteArray): Result<Unit> = runCatching {
            audioMediaIds.forEach { id ->
                val imageFileFullPath = saveAudioMediaImage(imageData).getOrThrow()
                updateAudioMedia(id = id, imageFileFullPath = imageFileFullPath).getOrThrow()
            }
        }
    }
}
