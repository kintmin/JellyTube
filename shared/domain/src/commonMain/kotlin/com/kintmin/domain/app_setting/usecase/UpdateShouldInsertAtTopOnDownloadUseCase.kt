package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog

class UpdateShouldInsertAtTopOnDownloadUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(value: Boolean): Result<Unit> {
        return appSettingRepository.updateShouldInsertAtTopOnDownload(value)
            .onSuccess { appLog.sendDebugLog(DebugLog("DownloadSetting", "다운로드 시 맨 위에 추가: $value")) }
            .onFailure { appLog.sendErrorLog(ErrorLog("DownloadSetting", "맨 위에 추가 설정 실패: $value", it)) }
    }
}
