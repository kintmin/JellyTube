package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

class BackupStepSensorUseCase constructor(
    private val stepRepository: StepRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(stepSensor: Long, rawCreatedTime: Long): Result<Unit> {
        return stepRepository.insertStepSensor(
            rawCreatedTime = rawCreatedTime,
            stepSensor = stepSensor,
        ).onFailure { appLog.sendErrorLog(ErrorLog("BackupStepSensorUseCase", "걸음 구간 저장 실패: $rawCreatedTime", it)) }
    }
}