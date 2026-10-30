package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kintmin.data.local_db.model.AudioMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioMediaDao {

    @Insert
    suspend fun insertAudioMedia(entity: AudioMediaEntity): Long

    @Query("SELECT * FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun getDataById(id: Int): AudioMediaEntity

    @Query("SELECT * FROM AUDIO_MEDIA WHERE source = :source")
    suspend fun getDataBySource(source: String): AudioMediaEntity

    @Query("SELECT * FROM AUDIO_MEDIA")
    fun getAudioMediaListFlow(): Flow<List<AudioMediaEntity>>

    @Query("SELECT * FROM AUDIO_MEDIA WHERE id NOT IN (SELECT audioMediaId FROM PLAYLIST_TRACK)")
    suspend fun getOrphanAudioMediaList(): List<AudioMediaEntity>

    /**
     * 플레이리스트 재생시간 합계는 증분으로 누적되므로(AudioMediaFacade 참고) 이 값이 곧 가감 delta다.
     * 편집본이 있는 음원은 편집본 길이가 실제 재생 길이라서 반드시 그쪽을 집계해야 한다.
     */
    @Query(
        """
SELECT COALESCE(SUM(COALESCE(editedAudioDurationSeconds, rawAudioDurationSeconds)), 0)
FROM AUDIO_MEDIA
WHERE id IN (:idList)
    """
    )
    suspend fun getTotalAudioDuration(idList: List<Int>): Long

    @Query(
        """
UPDATE AUDIO_MEDIA 
SET 
    name = COALESCE(:name, name),
    artist = COALESCE(:artist, artist),
    description = COALESCE(:description, description),
    imageFileNameWithExt = COALESCE(:imageFileNameWithExt, imageFileNameWithExt),
    lyricFileNameWithExt = COALESCE(:lyricFileNameWithExt, lyricFileNameWithExt)
WHERE id = :id
"""
    )
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileNameWithExt: String? = null,
        lyricFileNameWithExt: String? = null,
    )

    @Query("UPDATE AUDIO_MEDIA SET lyricFileNameWithExt = NULL WHERE id = :id")
    suspend fun clearLyricFile(id: Int)

    /** 편집본을 연결한다. 두 컬럼은 항상 같이 채운다. */
    @Query(
        """
UPDATE AUDIO_MEDIA
SET
    editedAudioFileNameWithExt = :editedAudioFileNameWithExt,
    editedAudioDurationSeconds = :editedAudioDurationSeconds
WHERE id = :id
"""
    )
    suspend fun applyEditedAudio(
        id: Int,
        editedAudioFileNameWithExt: String,
        editedAudioDurationSeconds: Long,
    )

    /** 편집본 연결을 해제해 원본으로 되돌린다. 두 컬럼은 항상 같이 비운다. */
    @Query(
        """
UPDATE AUDIO_MEDIA
SET
    editedAudioFileNameWithExt = NULL,
    editedAudioDurationSeconds = NULL
WHERE id = :id
"""
    )
    suspend fun clearEditedAudio(id: Int)

    @Query("UPDATE AUDIO_MEDIA SET tjKaraokeNumber = :tjKaraokeNumber WHERE id = :id")
    suspend fun updateKaraokeNumber(id: Int, tjKaraokeNumber: String)

    @Query("UPDATE AUDIO_MEDIA SET tjKaraokeNumber = NULL WHERE id = :id")
    suspend fun clearKaraokeNumber(id: Int)

    @Query("DELETE FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun deleteById(id: Int)
}