package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class UpdateLastStepSensorUseCase @Inject constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(lastStepSensor: Long): Result<Unit> {
        val today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE)
        stepRepository.updateLastStepSensorDate(today)
        return stepRepository.updateLastStepSensor(lastStepSensor)
    }
}