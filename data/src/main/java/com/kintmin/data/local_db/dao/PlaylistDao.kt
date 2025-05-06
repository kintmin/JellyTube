package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Query("UPDATE PLAYLIST SET name = :newName WHERE id = :id")
    suspend fun updatePlaylistName(id: Int, newName: String)

    @Query("UPDATE PLAYLIST SET description = :newDescription WHERE id = :id")
    suspend fun updatePlaylistDescription(id: Int, newDescription: String)

    @Query("UPDATE PLAYLIST SET audioMediaCount = :audioMediaCount, rawPlayTimeDuration = :totalPlayTime WHERE id = :id")
    suspend fun updatePlaylistPlayback(id: Int, audioMediaCount: Int, totalPlayTime: Long)
}