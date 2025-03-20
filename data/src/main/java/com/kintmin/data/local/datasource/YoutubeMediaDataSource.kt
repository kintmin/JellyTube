package com.kintmin.data.local.datasource

import com.kintmin.data.local.dao.AudioMediaDao
import com.kintmin.data.local.entity.AudioMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class YoutubeMediaDataSource @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
) {

    suspend fun getYoutubeData(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getMediaById(videoId)
        }
    }

    suspend fun saveYoutubeData(
        entity: AudioMediaEntity,
    ) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.insertMedia(entity)
        }
    }

    suspend fun deleteYoutubeData(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.deleteMediaById(videoId)
        }
    }
}