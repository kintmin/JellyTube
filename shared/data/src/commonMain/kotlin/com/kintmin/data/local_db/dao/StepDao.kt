package com.kintmin.data.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kintmin.data.local_db.model.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    /**
     * 어뷰저를 고려하여, Abort가 아닌 Replace로 최신 데이터를 유지하도록 설정
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StepEntity>)

    @Query("DELETE FROM STEP WHERE rawCreatedTime >= :startUtc AND rawCreatedTime <= :endUtc")
    suspend fun deleteBetween(startUtc: Long, endUtc: Long)

    /** 지우고 다시 적는 사이에 화면이 빈 구간을 보지 않게 한 번에 바꾼다. */
    @Transaction
    suspend fun replaceBetween(startUtc: Long, endUtc: Long, entities: List<StepEntity>) {
        deleteBetween(startUtc, endUtc)
        insertAll(entities)
    }

    @Query("SELECT * FROM STEP")
    suspend fun getAll(): List<StepEntity>

    /** 이전으로 들어온 기록은 이 기기의 기록을 덮지 않는다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringExisting(entities: List<StepEntity>)
}
