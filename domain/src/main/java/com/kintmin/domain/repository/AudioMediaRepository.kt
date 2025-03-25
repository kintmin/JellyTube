package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMediaData

interface AudioMediaRepository {
    suspend fun getListDataAll(): Result<List<AudioMediaData>>
    suspend fun getLocalData(id: String): Result<AudioMediaData>
    suspend fun downloadData(downloadUrl: String, id: String): Result<AudioMediaData>
    suspend fun saveDataToLocal(data: AudioMediaData): Result<Unit>
    suspend fun deleteMediaFile(id: String): Result<Unit>
    suspend fun clearFileCache(): Result<Unit>
}