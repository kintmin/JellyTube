package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kintmin.data.local_db.model.PlaylistTrackEntity
import com.kintmin.data.local_db.model.PlaylistTrackFullDto
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(entity: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrackList(entities: List<PlaylistTrackEntity>)

    @Transaction
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    fun getPlaylistTrackFullListFlow(playlistId: Int): Flow<List<PlaylistTrackFullDto>>

    @Transaction
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId AND audioMediaId = :audioMediaId")
    fun getPlaylistTrackFullFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackFullDto>

    @Transaction
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId ORDER BY sequence LIMIT 1")
    suspend fun getFirstAudioMedia(playlistId: Int): PlaylistTrackFullDto

    @Query("SELECT audioMediaId FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun getAudioMediaIdList(playlistId: Int): List<Int>

    @Query("SELECT playlistId FROM PLAYLIST_TRACK WHERE audioMediaId = :audioMediaId")
    fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>>

    @Query("SELECT COALESCE(MAX(sequence), 0) + 1 FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun getNextSequence(playlistId: Int): Int

    @Query(
        """
UPDATE PLAYLIST_TRACK
SET sequence = (CASE
    WHEN audioMediaId = :audioMediaId THEN :newSequence
    WHEN sequence >= :newSequence THEN sequence + 1
    ELSE sequence
END)
WHERE playlistId = :playlistId
"""
    )
    suspend fun updateSequence(playlistId: Int, audioMediaId: Int, newSequence: Int)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE playlistId = :playlistId AND audioMediaId = :audioMediaId")
    suspend fun deletePlaylistTrackMedia(playlistId: Int, audioMediaId: Int)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE playlistId = :playlistId AND audioMediaId IN (:audioMediaIdList)")
    suspend fun deletePlaylistTracks(playlistId: Int, audioMediaIdList: List<Int>)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTrack(playlistId: Int)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE audioMediaId = :audioMediaId")
    suspend fun deleteAudioMedia(audioMediaId: Int)
}