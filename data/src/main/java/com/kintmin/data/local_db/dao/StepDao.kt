package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kintmin.data.local_db.model.StepEntity

@Dao
interface StepDao {

    /**
     * 어뷰저를 고려하여, Abort가 아닌 Replace로 늘 최신 데이터를 유지하도록 설정
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StepEntity)

    @Query(
        """
        SELECT stepSensor
        FROM STEP
        ORDER BY rawCreatedTime DESC
        LIMIT 1
    """
    )
    suspend fun getLastStepSensor(): Long?

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
}