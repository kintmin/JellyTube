package com.kintmin.localdatabase.dataSource

import com.kintmin.localdatabase.entity.PlaylistEntity

interface LocalPlaylistDataSource {
    suspend fun getEntityListAll(): Result<List<PlaylistEntity>>
    suspend fun getEntity(id: Int): Result<PlaylistEntity>
    suspend fun insertEntity(entity: PlaylistEntity): Result<Unit>
}