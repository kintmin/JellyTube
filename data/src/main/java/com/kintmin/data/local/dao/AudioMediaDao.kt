package com.kintmin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kintmin.data.local.entity.AudioMediaEntity

@Dao
interface AudioMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(video: AudioMediaEntity)

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: String): AudioMediaEntity?

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMediaById(id: String)
}