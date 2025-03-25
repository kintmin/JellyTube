package com.kintmin.localdatabase.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kintmin.localdatabase.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM PLAYLIST WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Int): PlaylistEntity

    @Query("SELECT * FROM PLAYLIST")
    suspend fun getPlaylistAll(): List<PlaylistEntity>
}