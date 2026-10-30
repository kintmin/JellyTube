package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kintmin.data.local_db.model.AudioEditProjectEntity

@Dao
interface AudioEditProjectDao {

    /** 편집 화면에서만 읽는다. 편집본을 만든 적 없으면 null. */
    @Query("SELECT * FROM AUDIO_EDIT_PROJECT WHERE audioMediaId = :audioMediaId")
    suspend fun getByAudioMediaId(audioMediaId: Int): AudioEditProjectEntity?

    /** 편집본을 다시 적용하면 이전 편집 상태를 덮어쓴다. */
    @Upsert
    suspend fun upsert(entity: AudioEditProjectEntity)

    @Query("DELETE FROM AUDIO_EDIT_PROJECT WHERE audioMediaId = :audioMediaId")
    suspend fun deleteByAudioMediaId(audioMediaId: Int)
}
