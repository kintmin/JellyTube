package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kintmin.data.local_db.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM PLAYLIST WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Int): PlaylistEntity

    @Query("SELECT * FROM PLAYLIST")
    suspend fun getPlaylistAll(): List<PlaylistEntity>
}