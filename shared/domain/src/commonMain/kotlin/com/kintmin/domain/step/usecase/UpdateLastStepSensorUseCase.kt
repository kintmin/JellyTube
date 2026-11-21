package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.extension.todayLocalDate
import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

class UpdateLastStepSensorUseCase constructor(
    private val stepRepository: StepRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(lastStepSensor: Long): Result<Unit> {
        val today = todayLocalDate().toBasicIsoString()
        val dateResult = stepRepository.updateLastStepSensorDate(today)
        return stepRepository.updateLastStepSensor(lastStepSensor)
            .mapCatching { dateResult.getOrThrow() }
            .onFailure { appLog.sendErrorLog(ErrorLog("UpdateLastStepSensorUseCase", "마지막 걸음 센서 값 저장 실패", it)) }
    }
}
