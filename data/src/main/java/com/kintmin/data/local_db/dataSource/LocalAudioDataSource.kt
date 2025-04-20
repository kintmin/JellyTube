package com.kintmin.data.local_db.dataSource

import com.kintmin.data.local_db.entity.AudioMediaEntity

interface LocalAudioDataSource {
    suspend fun getEntityListAll(): Result<List<AudioMediaEntity>>
    suspend fun getEntityListByPlaylistId(playlistId: Int): Result<List<AudioMediaEntity>>
    suspend fun getEntity(id: String): Result<AudioMediaEntity>
    suspend fun insertEntity(entity: AudioMediaEntity): Result<Unit>
    suspend fun deleteEntity(id: String): Result<Unit>
}