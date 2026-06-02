package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import kotlinx.coroutines.flow.Flow

class FetchIsStepEnabledFlowUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return appSettingRepository.getIsStepEnabledFlow()
    }
}
