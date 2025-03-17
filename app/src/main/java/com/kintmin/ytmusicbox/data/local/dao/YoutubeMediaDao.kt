package com.kintmin.ytmusicbox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kintmin.ytmusicbox.data.local.entity.YoutubeMediaEntity

@Dao
interface YoutubeMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(video: YoutubeMediaEntity)

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getMediaById(id: String): YoutubeMediaEntity?

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMediaById(id: String)
}