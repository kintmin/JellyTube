package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kintmin.data.local_db.model.AudioMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioMediaDao {
    @Insert
    suspend fun insert(entity: AudioMediaEntity): Long

    @Query("SELECT * FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun getDataById(id: Int): AudioMediaEntity

    @Query("SELECT * FROM AUDIO_MEDIA WHERE source = :source")
    suspend fun getDataBySource(source: String): AudioMediaEntity

    @Query("SELECT * FROM AUDIO_MEDIA")
    fun getDataListFlow(): Flow<List<AudioMediaEntity>>

    @Query("DELETE FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun deleteById(id: Int)
}