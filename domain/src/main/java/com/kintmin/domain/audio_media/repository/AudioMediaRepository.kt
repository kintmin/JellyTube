package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun downloadAudioMedia(downloadUrl: String): Result<DownloadedMedia>
    suspend fun addAudioMedia(
        downloadedAudioMedia: DownloadedMedia,
        playlistIdOnDownload: Int,
        shouldInsertAtTopOnDownload: Boolean,
    ): Result<Pair<AudioMedia, Int>>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    fun getAudioMediaListFlow(): Flow<List<AudioMedia>>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
    ): Result<Unit>

    suspend fun deleteDownloadedFile(downloadedAudioMedia: DownloadedMedia): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>
}
