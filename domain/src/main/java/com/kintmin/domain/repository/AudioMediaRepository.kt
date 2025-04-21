package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMedia

interface AudioMediaRepository {
    suspend fun addAudioMedia(newAudioMedia: AudioMedia): Result<Unit>
    suspend fun getAudioMediaList(): Result<List<AudioMedia>>
    suspend fun getAudioMedia(id: String): Result<AudioMedia>
    suspend fun downloadAudioMedia(downloadUrl: String, id: String): Result<AudioMedia>
    suspend fun updateAudioMedia(id: String, newAudioMedia: AudioMedia): Result<Unit>
    suspend fun deleteAudioMediaInvalidCache(id: String): Result<Unit>
}