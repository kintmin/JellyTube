package com.kintmin.localdatabase.dataSourceImpl

import com.kintmin.localdatabase.dao.PlaylistDao
import com.kintmin.localdatabase.dataSource.LocalPlaylistDataSource
import com.kintmin.localdatabase.entity.PlaylistEntity
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