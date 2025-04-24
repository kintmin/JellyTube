package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kintmin.data.local_db.entity.AudioMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AudioMediaEntity)

    @Query("SELECT * FROM AUDIO_MEDIA WHERE id = :id LIMIT 1")
    suspend fun getDataById(id: String): AudioMediaEntity

    @Query("SELECT * FROM AUDIO_MEDIA")
    fun getDataListFlow(): Flow<List<AudioMediaEntity>>

    @Query("SELECT * FROM AUDIO_MEDIA WHERE playlistId = :playlistId")
    suspend fun getDataListByPlaylistId(playlistId: Int): List<AudioMediaEntity>

    @Query("DELETE FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun deleteById(id: String)
}