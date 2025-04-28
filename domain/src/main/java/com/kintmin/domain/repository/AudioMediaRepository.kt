package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMedia
import com.kintmin.domain.model.DownloadedAudioMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun addAudioMedia(newAudioMedia: DownloadedAudioMedia): Result<Unit>
    fun getAudioMediaListFlow(playlistId: Int): Flow<List<AudioMedia>>
    suspend fun isExistAudioMedia(source: String): Result<Boolean>
    suspend fun downloadAudioMedia(downloadUrl: String, source: String): Result<DownloadedAudioMedia>
    suspend fun deleteInvalidAudioMediaFile(fileName: String): Result<Unit>
    suspend fun deleteAudioMedia(id: Int): Result<Unit>
}