package com.kintmin.domain.repository

import androidx.paging.PagingData
import com.kintmin.domain.model.AudioMedia
import kotlinx.coroutines.flow.Flow

interface AudioMediaRepository {
    suspend fun addAudioMedia(newAudioMedia: AudioMedia): Result<Unit>
    fun getPagingAudioMediaFlow(): Flow<PagingData<AudioMedia>>
    suspend fun getAudioMediaList(): Result<List<AudioMedia>>
    suspend fun getAudioMedia(id: String): Result<AudioMedia>
    suspend fun downloadAudioMedia(downloadUrl: String, id: String): Result<AudioMedia>
    suspend fun updateAudioMedia(id: String, newAudioMedia: AudioMedia): Result<Unit>
    suspend fun deleteAudioMediaInvalidCache(id: String): Result<Unit>
    suspend fun deleteAudioMedia(id: String): Result<Unit>
}