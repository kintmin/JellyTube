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

    @Query("SELECT * FROM AUDIO_MEDIA")
    suspend fun getAll(): List<AudioMediaEntity>

    @Query("SELECT * FROM AUDIO_MEDIA WHERE id NOT IN (SELECT audioMediaId FROM PLAYLIST_TRACK)")
    suspend fun getOrphanAudioMediaList(): List<AudioMediaEntity>

    /**
     * 편집본이 있는 음원은 편집본 길이가 실제 재생 길이다.
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
    lyricFileNameWithExt = COALESCE(:lyricFileNameWithExt, lyricFileNameWithExt)
WHERE id = :id
"""
    )
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        lyricFileNameWithExt: String? = null,
    )

    @Query("UPDATE AUDIO_MEDIA SET customImageFileNameWithExt = :customImageFileNameWithExt WHERE id = :id")
    suspend fun applyCustomImage(id: Int, customImageFileNameWithExt: String)

    /** 고른 썸네일을 떼어 정본으로 되돌린다. */
    @Query("UPDATE AUDIO_MEDIA SET customImageFileNameWithExt = NULL WHERE id = :id")
    suspend fun clearCustomImage(id: Int)

    /** 정본을 다시 정한다. 고른 썸네일이 가리면 바꾼 것이 보이지 않으므로 같이 뗀다. */
    @Query(
        """
UPDATE AUDIO_MEDIA
SET
    imageFileNameWithExt = :imageFileNameWithExt,
    customImageFileNameWithExt = NULL
WHERE id = :id
"""
    )
    suspend fun replaceImage(id: Int, imageFileNameWithExt: String)

    @Query("UPDATE AUDIO_MEDIA SET audioFileNameWithExt = :fileNameWithExt WHERE id = :id")
    suspend fun updateAudioFileName(id: Int, fileNameWithExt: String)

    @Query("UPDATE AUDIO_MEDIA SET editedAudioFileNameWithExt = :fileNameWithExt WHERE id = :id")
    suspend fun updateEditedAudioFileName(id: Int, fileNameWithExt: String)

    @Query("UPDATE AUDIO_MEDIA SET lyricFileNameWithExt = NULL WHERE id = :id")
    suspend fun clearLyricFile(id: Int)

    @Query("UPDATE AUDIO_MEDIA SET editedLyricFileNameWithExt = :editedLyricFileNameWithExt WHERE id = :id")
    suspend fun applyEditedLyricFile(id: Int, editedLyricFileNameWithExt: String)

    @Query("UPDATE AUDIO_MEDIA SET editedLyricFileNameWithExt = NULL WHERE id = :id")
    suspend fun clearEditedLyricFile(id: Int)

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