package com.kintmin.data.local_db.dataSourceImpl

import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.dataSource.LocalPlaylistDataSource
import com.kintmin.data.local_db.entity.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LocalPlaylistDataSourceImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
): LocalPlaylistDataSource {
    override suspend fun getEntity(id: Int) = runCatching {
        withContext(Dispatchers.IO) {
            playlistDao.getPlaylistById(id)
        }
    }

    override suspend fun getEntityListAll() = runCatching {
        withContext(Dispatchers.IO) {
            playlistDao.getPlaylistAll()
        }
    }

    override suspend fun insertEntity(entity: PlaylistEntity) = runCatching {
        withContext(Dispatchers.IO) {
            playlistDao.insertPlaylist(entity)
        }
    }
}