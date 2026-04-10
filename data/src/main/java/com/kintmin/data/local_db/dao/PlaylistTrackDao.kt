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
    suspend fun insertPlaylistTrackList(entities: List<PlaylistTrackEntity>)

    @Transaction
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId ORDER BY sequence")
    fun getPlaylistTrackFullListFlow(playlistId: Int): Flow<List<PlaylistTrackFullDto>>

    @Transaction
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId AND audioMediaId = :audioMediaId")
    fun getPlaylistTrackFullFlow(playlistId: Int, audioMediaId: Int): Flow<PlaylistTrackFullDto>

    /**
     * withTransaction 에서 실행되기 위해 Dto지만 Transaction 없이 값을 가져온다.
     */
    @Query("SELECT * FROM PLAYLIST_TRACK WHERE playlistId = :playlistId ORDER BY sequence LIMIT 1")
    suspend fun getFirstAudioMediaWithNoLock(playlistId: Int): PlaylistTrackFullDto

    @Query("SELECT playlistId FROM PLAYLIST_TRACK WHERE audioMediaId = :audioMediaId")
    fun getPlaylistIdListFlow(audioMediaId: Int): Flow<List<Int>>

    @Query("SELECT playlistId FROM PLAYLIST_TRACK WHERE audioMediaId = :audioMediaId")
    suspend fun getLinkedPlaylistIdList(audioMediaId: Int): List<Int>

    @Query("SELECT audioMediaId FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun getLinkedAudioMediaIdList(playlistId: Int): List<Int>

    @Query("SELECT COUNT(*) FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: Int): Int

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun getMaxSequence(playlistId: Int): Int

    @Query("UPDATE PLAYLIST_TRACK SET sequence = sequence + :offset WHERE playlistId = :playlistId")
    suspend fun increaseSequenceAll(playlistId: Int, offset: Int)

    @Query(
        """
UPDATE PLAYLIST_TRACK
SET sequence = (CASE
    WHEN audioMediaId = :audioMediaId THEN :newSequence
    WHEN :newSequence < :oldSequence AND sequence BETWEEN :newSequence AND (:oldSequence - 1) THEN sequence + 1
    WHEN :newSequence > :oldSequence AND sequence BETWEEN (:oldSequence + 1) AND :newSequence THEN sequence - 1
    ELSE sequence
END)
WHERE playlistId = :playlistId
"""
    )
    suspend fun updateSequence(playlistId: Int, audioMediaId: Int, oldSequence: Int, newSequence: Int)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE playlistId = :playlistId AND audioMediaId IN (:audioMediaIdList)")
    suspend fun deletePlaylistTracks(playlistId: Int, audioMediaIdList: List<Int>)

    @Query("DELETE FROM PLAYLIST_TRACK WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTrackByPlaylistId(playlistId: Int)
}
