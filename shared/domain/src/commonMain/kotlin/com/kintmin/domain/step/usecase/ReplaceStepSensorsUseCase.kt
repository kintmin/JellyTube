package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.model.StepData
import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.ErrorLog

class ReplaceStepSensorsUseCase constructor(
    private val stepRepository: StepRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(startMillis: Long, endMillis: Long, stepDataList: List<StepData>): Result<Unit> {
        return stepRepository.replaceStepSensors(
            startMillis = startMillis,
            endMillis = endMillis,
            stepDataList = stepDataList,
        ).onFailure { appLog.sendErrorLog(ErrorLog("ReplaceStepSensorsUseCase", "걸음 구간 바꾸기 실패: $startMillis~$endMillis", it)) }
    }
}
