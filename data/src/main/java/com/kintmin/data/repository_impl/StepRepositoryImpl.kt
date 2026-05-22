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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
                val (start, end) = dateToRangeMillis(date)
                stepDao.getEntitiesBetween(
                    startUtc = start,
                    endUtc = end,
                ).map { it.toStepData() }
            }
        }
    }

    override fun getStepDataListByDateFlow(date: String): Flow<List<StepData>> {
        val (start, end) = dateToRangeMillis(date)
        return stepDao.getEntitiesBetweenFlow(start, end)
            .map { entities -> entities.map { it.toStepData() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getStepDataListInRangeFlow(startMillis: Long, endMillis: Long): Flow<List<StepData>> {
        return stepDao.getEntitiesBetweenFlow(startMillis, endMillis)
            .map { entities -> entities.map { it.toStepData() } }
            .flowOn(Dispatchers.IO)
    }

    private fun dateToRangeMillis(date: String): Pair<Long, Long> {
        val targetDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE)
        val zoneId = ZoneId.systemDefault()
        val start = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    private fun StepEntity.toStepData() = StepData(
        rawCreatedTime = rawCreatedTime,
        stepSensor = stepSensor,
    )
}