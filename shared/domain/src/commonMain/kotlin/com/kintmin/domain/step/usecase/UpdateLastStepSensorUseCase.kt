package com.kintmin.domain.step.usecase

import com.kintmin.domain.extension.toBasicIsoString
import com.kintmin.domain.extension.todayLocalDate
import com.kintmin.domain.step.repository.StepRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog

class UpdateLastStepSensorUseCase constructor(
    private val stepRepository: StepRepository,
    private val appLog: AppLog,
) {

    suspend operator fun invoke(lastStepSensor: Long): Result<Unit> {
        val today = todayLocalDate().toBasicIsoString()
        val dateResult = stepRepository.updateLastStepSensorDate(today)
        return stepRepository.updateLastStepSensor(lastStepSensor)
            .mapCatching { dateResult.getOrThrow() }
            // 센서 값이 바뀔 때마다 불려, 실패가 이어지면 걸음마다 Issue 가 쌓인다.
            .onFailure { appLog.sendDebugLog(DebugLog("UpdateLastStepSensorUseCase", "마지막 걸음 센서 값 저장 실패: $it")) }
    }
}
