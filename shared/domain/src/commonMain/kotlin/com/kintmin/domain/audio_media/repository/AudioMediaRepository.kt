package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun downloadAudioMedia(downloadUrl: String): Result<DownloadedMedia>
    suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    fun getAudioMediaListFlow(): Flow<List<AudioMedia>>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
    ): Result<Unit>

    suspend fun saveImage(imageData: ByteArray): Result<String>

    suspend fun importSharedAudio(
        contentUriString: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>

    suspend fun importUploadedAudio(
        bytes: ByteArray,
        originalFileName: String,
        playlistIdOnDownload: Int?,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<AddedAudioMedia>

    suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>

    suspend fun deleteOrphanAudioMedia(): Result<List<AudioMedia>>
    suspend fun deleteFile(fileNameWithExt: String): Result<Unit>
    suspend fun listAudioAndImageFileNames(): Result<List<String>>
}
