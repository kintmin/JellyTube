package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.data.local_db.dao.StepDao
import com.kintmin.data.local_db.model.StepEntity
import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * dataStore - TodayStepCount: 오늘 걸음수를 보장
 * dataStore - LastStepSensor: 마지막으로 측정된 센서값을 보장
 * roomDB - StepSensor: 분 단위 센서값을 보장
 */
@Singleton
class StepRepositoryImpl @Inject constructor(
    private val stepDao: StepDao,
    private val datastoreUtil: DatastoreUtil,
): StepRepository {

    override fun getTodayStepCount(): Flow<Int?> {
        return datastoreUtil.getData(PreferencesKey.TodayStepCount)
    }

    override fun getLastStepSensor(): Flow<Long?> {
        return datastoreUtil.getData(PreferencesKey.LastStepSensor)
    }

    override fun getAccelerateStep(): Flow<Int?> {
        return datastoreUtil.getData(PreferencesKey.AccelerateStep)
    }

    override suspend fun updateTodayStepCount(newStep: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.TodayStepCount, newStep)
    }

    override suspend fun updateLastStepSensor(newSensor: Long): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.LastStepSensor, newSensor)
    }

    override suspend fun updateAccelerateStep(newStep: Int): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.AccelerateStep, newStep)
    }

    override suspend fun insertStepSensor(rawCreatedTime: Long, stepSensor: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                stepDao.insert(
                    StepEntity(
                        rawCreatedTime = rawCreatedTime,
                        stepSensor = stepSensor
                    )
                )
            }
        }
    }

    override suspend fun getStepDataListByDate(date: String): Result<List<StepData>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val targetDate = LocalDate.parse(
                    date,
                    DateTimeFormatter.BASIC_ISO_DATE // yyyyMMdd
                )

                val start = targetDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val end = targetDate
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                stepDao.getEntitiesBetween(
                    startUtc = start,
                    endUtc = end,
                ).map {
                    StepData(
                        rawCreatedTime = it.rawCreatedTime,
                        stepSensor = it.stepSensor,
                    )
                }
            }
        }
    }

    override suspend fun deleteEntitiesOlderThan15DaysFromTodayMidnight(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val cutoff = LocalDate.now(ZoneId.systemDefault())
                    .minusDays(15)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                stepDao.deleteOlderThan(cutoff)
            }
        }
    }
}