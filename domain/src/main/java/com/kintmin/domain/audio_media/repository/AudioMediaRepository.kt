package com.kintmin.domain.audio_media.repository

import com.kintmin.domain.audio_media.model.AudioMedia

interface AudioMediaRepository {
    suspend fun addAudioMedia(downloadUrl: String): Result<AudioMedia>
    suspend fun getAudioMediaBySource(source: String): Result<AudioMedia>
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileNameWithExt: String? = null,
    ): Result<Unit>

    suspend fun deleteAudioMedia(id: Int): Result<Unit>
}