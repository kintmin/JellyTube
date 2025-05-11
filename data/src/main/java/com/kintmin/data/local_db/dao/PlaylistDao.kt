package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kintmin.data.local_db.model.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM PLAYLIST WHERE id = :id")
    suspend fun getPlaylistById(id: Int): PlaylistEntity

    @Query("SELECT * FROM PLAYLIST")
    fun getPlaylistListFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM PLAYLIST WHERE id = :id")
    fun getPlaylistFlow(id: Int): Flow<PlaylistEntity>

    @Update
    suspend fun updatePlaylist(entity: PlaylistEntity)

    @Query("DELETE FROM PLAYLIST WHERE id = :id")
    suspend fun deletePlaylistName(id: Int)
}