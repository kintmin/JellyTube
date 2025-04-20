package com.kintmin.data.local_db.dataSourceImpl

import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.dataSource.LocalAudioDataSource
import com.kintmin.data.local_db.entity.AudioMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalAudioDataSourceImpl @Inject constructor(
    private val audioMediaDao: AudioMediaDao,
): LocalAudioDataSource {
    override suspend fun getEntityListAll() = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getDataListAll()
        }
    }

    override suspend fun getEntityListByPlaylistId(playlistId: Int) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getDataListByPlaylistId(playlistId)
        }
    }

    override suspend fun getEntity(id: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.getDataById(id)
        }
    }

    override suspend fun insertEntity(entity: AudioMediaEntity) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.insert(entity)
        }
    }

    override suspend fun deleteEntity(id: String) = runCatching {
        withContext(Dispatchers.IO) {
            audioMediaDao.deleteById(id)
        }
    }
}