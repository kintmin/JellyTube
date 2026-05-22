package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import javax.inject.Inject

class UpdateIsStepEnabledUseCase @Inject constructor(
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(value: Boolean): Result<Unit> {
        return appSettingRepository.updateIsStepEnabled(value)
    }
}
