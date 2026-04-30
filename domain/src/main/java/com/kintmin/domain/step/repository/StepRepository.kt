package com.kintmin.domain.step.repository

import com.kintmin.domain.step.model.StepData
import kotlinx.coroutines.flow.Flow

interface StepRepository {

    fun getTodayStepCount(): Flow<Int?>
    fun getLastStepSensor(): Flow<Long?>
    fun getAccelerateStep(): Flow<Int?>

    suspend fun updateTodayStepCount(newStep: Int): Result<Unit>
    suspend fun updateLastStepSensor(newSensor: Long): Result<Unit>
    suspend fun updateAccelerateStep(newStep: Int): Result<Unit>

    suspend fun insertStepSensor(rawCreatedTime: Long, stepSensor: Long): Result<Unit>
    suspend fun getStepDataListByDate(date: String): Result<List<StepData>>
    suspend fun deleteEntitiesOlderThan15DaysFromTodayMidnight(): Result<Unit>
}