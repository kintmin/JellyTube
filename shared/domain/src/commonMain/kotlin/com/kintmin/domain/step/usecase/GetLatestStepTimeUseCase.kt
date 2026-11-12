package com.kintmin.domain.step.usecase

import com.kintmin.domain.step.repository.StepRepository
import kotlinx.coroutines.flow.first

/** 구간 안에서 가장 늦게 적힌 걸음수 기록의 시각. 없으면 null. */
class GetLatestStepTimeUseCase constructor(
    private val stepRepository: StepRepository,
) {

    suspend operator fun invoke(startMillis: Long, endMillis: Long): Long? {
        return stepRepository.getStepDataListInRangeFlow(startMillis, endMillis)
            .first()
            .maxOfOrNull { it.rawCreatedTime }
    }
}
