package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun addAudioMedia(downloadUrl: String): Result<AudioMedia>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    fun getAudioMediaListFlow(): Flow<List<AudioMedia>>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileFullPath: String? = null,
    ): Result<Unit>

    suspend fun deleteAudioMedia(id: Int): Result<List<Int>>
}