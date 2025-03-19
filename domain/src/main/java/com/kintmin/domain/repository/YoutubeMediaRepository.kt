package com.kintmin.domain.repository

import com.kintmin.domain.model.AudioMediaData

interface YoutubeMediaRepository {
    fun isExistFile(videoId: String): Result<Boolean>
    suspend fun isExistData(videoId: String): Result<Boolean>
    suspend fun deleteData(videoId: String): Result<Unit>
    suspend fun saveData(mediaData: AudioMediaData): Result<Unit>
    suspend fun getCachedMediaData(videoId: String): Result<AudioMediaData>
    suspend fun getMediaData(videoId: String): Result<AudioMediaData>
    suspend fun downloadThumbnail(url: String, videoId: String): Result<String>
}