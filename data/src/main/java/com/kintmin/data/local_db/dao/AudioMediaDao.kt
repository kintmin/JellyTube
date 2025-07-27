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

    @Query("""
UPDATE AUDIO_MEDIA 
SET 
    name = COALESCE(:name, name),
    artist = COALESCE(:artist, artist),
    description = COALESCE(:description, description),
    imageFileNameWithExt = COALESCE(:imageFileNameWithExt, imageFileNameWithExt)
WHERE id = :id
""")
    suspend fun updateAudioMedia(
        id: Int,
        name: String? = null,
        artist: String? = null,
        description: String? = null,
        imageFileNameWithExt: String? = null,
    )

    @Query("DELETE FROM AUDIO_MEDIA WHERE id = :id")
    suspend fun deleteById(id: Int)
}