package com.kintmin.data.local_db.dataSource

import com.kintmin.data.local_db.entity.PlaylistEntity

interface LocalPlaylistDataSource {
    suspend fun getEntityListAll(): Result<List<PlaylistEntity>>
    suspend fun getEntity(id: Int): Result<PlaylistEntity>
    suspend fun insertEntity(entity: PlaylistEntity): Result<Unit>
}