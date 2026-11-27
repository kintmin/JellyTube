package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository

class MarkBackupNoticeShownUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(): Result<Unit> = appSettingRepository.markBackupNoticeShown()
}
