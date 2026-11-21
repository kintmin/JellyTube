package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.DebugLog
import com.kintmin.log.model.ErrorLog

class UpdatePlaylistIdOnDownloadUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return appSettingRepository.updatePlaylistIdOnDownload(playlistId)
            .onSuccess { appLog.sendDebugLog(DebugLog("DownloadSetting", "다운로드 대상 플레이리스트: $playlistId")) }
            .onFailure { appLog.sendErrorLog(ErrorLog("DownloadSetting", "다운로드 대상 바꾸기 실패: $playlistId", it)) }
    }
}
