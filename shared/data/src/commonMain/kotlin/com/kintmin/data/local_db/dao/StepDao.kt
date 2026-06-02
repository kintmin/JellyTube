package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kintmin.data.local_db.model.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    /**
     * ?´ë·°?€ë¥?ê³ ë ¤?˜ì—¬, Abortê°€ ?„ë‹Œ Replaceë¡???ìµœì‹  ?°ì´?°ë? ? ì??˜ë„ë¡??¤ì •
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StepEntity)

    @Query(
        """
        SELECT *
        FROM STEP
        WHERE rawCreatedTime >= :startUtc
            AND rawCreatedTime <= :endUtc
        ORDER BY rawCreatedTime ASC
    """
    )
    suspend fun getEntitiesBetween(startUtc: Long, endUtc: Long): List<StepEntity>

    @Query(
        """
        SELECT *
        FROM STEP
        WHERE rawCreatedTime >= :startUtc
            AND rawCreatedTime <= :endUtc
        ORDER BY rawCreatedTime ASC
    """
    )
    fun getEntitiesBetweenFlow(startUtc: Long, endUtc: Long): Flow<List<StepEntity>>
}