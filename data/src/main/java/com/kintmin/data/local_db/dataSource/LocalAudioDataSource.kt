package com.kintmin.data.local_db.dataSource

import androidx.paging.PagingData
import com.kintmin.data.local_db.entity.AudioMediaEntity
import kotlinx.coroutines.flow.Flow

interface LocalAudioDataSource {
    fun getPagingEntityFlow(): Flow<PagingData<AudioMediaEntity>>
    suspend fun getEntityListByPlaylistId(playlistId: Int): Result<List<AudioMediaEntity>>
    suspend fun getEntity(id: String): Result<AudioMediaEntity>
    suspend fun insertEntity(entity: AudioMediaEntity): Result<Unit>
    suspend fun deleteEntity(id: String): Result<Unit>
}