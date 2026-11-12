package com.kintmin.domain.step.repository

import com.kintmin.domain.step.model.StepData
import kotlinx.coroutines.flow.Flow

interface StepRepository {

    fun getLastStepSensorForToday(today: String): Flow<Long?>
    fun getAccelerateStep(): Flow<Int?>

    suspend fun updateLastStepSensor(newSensor: Long): Result<Unit>
    suspend fun updateLastStepSensorDate(date: String): Result<Unit>
    suspend fun updateAccelerateStep(newStep: Int): Result<Unit>

    suspend fun insertStepSensor(rawCreatedTime: Long, stepSensor: Long): Result<Unit>
    /** [startMillis]~[endMillis](양끝 포함)의 기록을 지우고 [stepDataList] 로 바꾼다. */
    suspend fun replaceStepSensors(startMillis: Long, endMillis: Long, stepDataList: List<StepData>): Result<Unit>
    suspend fun getStepDataListByDate(date: String): Result<List<StepData>>
    fun getStepDataListByDateFlow(date: String): Flow<List<StepData>>
    fun getStepDataListInRangeFlow(startMillis: Long, endMillis: Long): Flow<List<StepData>>
}