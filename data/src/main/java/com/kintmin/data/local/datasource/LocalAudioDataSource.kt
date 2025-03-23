package com.kintmin.data.local.datasource

import com.kintmin.data.local.dao.AudioMediaDao
import com.kintmin.data.local.entity.AudioMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalAudioDataSource @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
) {
    suspend fun getEntityList() = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getMediaList()
        }
    }

    suspend fun getEntity(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getMediaById(videoId)
        }
    }

    suspend fun insertEntity(entity: AudioMediaEntity) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.insertMedia(entity)
        }
    }

    suspend fun deleteEntity(videoId: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.deleteMediaById(videoId)
        }
    }
}