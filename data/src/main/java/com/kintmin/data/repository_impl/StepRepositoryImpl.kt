package com.kintmin.data.repository_impl

import com.kintmin.data.local_datastore.DatastoreUtil
import com.kintmin.data.local_datastore.PreferencesKey
import com.kintmin.data.local_db.dao.StepDao
import com.kintmin.data.local_db.model.StepEntity
import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepRepositoryImpl @Inject constructor(
    private val stepDao: StepDao,
    private val datastoreUtil: DatastoreUtil,
): StepRepository {

    override fun getLastStepSensorForToday(today: String): Flow<Long?> {
        return combine(
            datastoreUtil.getData(PreferencesKey.LastStepSensor),
            datastoreUtil.getData(PreferencesKey.LastStepSensorDate),
        ) { sensor, date -> if (date == today) sensor else null }
    }

    override fun getAccelerateStep(): Flow<Int?> {
        return datastoreUtil.getData(PreferencesKey.AccelerateStep)
    }

    override suspend fun updateLastStepSensor(newSensor: Long): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.LastStepSensor, newSensor)
    }

    override suspend fun updateLastStepSensorDate(date: String): Result<Unit> {
        return datastoreUtil.updateData(PreferencesKey.LastStepSensorDate, date)
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
}