package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog
import kotlinx.coroutines.flow.firstOrNull

class GetStepCountUseCase constructor(
    private val stepRepository: StepRepository,
    private val calculateStepCountUseCase: CalculateStepCountUseCase,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(date: String): Int {
        val lastStepSensor = stepRepository.getLastStepSensorForToday(date).firstOrNull()
        val steps = stepRepository.getStepDataListByDate(date)
            .onFailure { appLog.sendErrorLog(ErrorLog("GetStepCountUseCase", "걸음 기록 읽기 실패: $date", it)) }
            .getOrDefault(emptyList()).map {
            it.stepSensor
        }.toMutableList()
        val resultStepList = lastStepSensor?.let {
            steps + it
        } ?: steps
        return calculateStepCountUseCase(resultStepList)
    }
}