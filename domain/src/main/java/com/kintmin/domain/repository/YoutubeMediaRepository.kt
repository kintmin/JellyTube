package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMediaData

interface YoutubeMediaRepository {
    suspend fun getMediaData(youtubeUrl: String, videoId: String): Result<AudioMediaData>
    suspend fun getMediaDataFromMetaData(videoId: String): Result<AudioMediaData>
    suspend fun saveMetaData(mediaData: AudioMediaData): Result<Unit>
    suspend fun deleteMediaData(videoId: String): Result<Unit>
    fun clearCacheData(): Result<Unit>
}