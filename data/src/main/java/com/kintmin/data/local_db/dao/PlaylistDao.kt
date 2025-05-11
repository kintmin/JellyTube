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

    @Query("""
UPDATE PLAYLIST
SET 
    name = COALESCE(:name, name),
    description = COALESCE(:description, description),
    audioMediaCount = COALESCE(:audioMediaCount, audioMediaCount),
    rawPlayTimeDuration = COALESCE(:rawPlayTimeDuration, rawPlayTimeDuration),
    imageFileNameWithExt = COALESCE(:imageFileNameWithExt, imageFileNameWithExt)
WHERE id = :id
""")
    suspend fun updatePlaylist(
        id: Int,
        name: String? = null,
        description: String? = null,
        audioMediaCount: Int? = null,
        rawPlayTimeDuration: Long? = null,
        imageFileNameWithExt: String? = null,
    )

    @Query("DELETE FROM PLAYLIST WHERE id = :id")
    suspend fun deletePlaylistName(id: Int)
}