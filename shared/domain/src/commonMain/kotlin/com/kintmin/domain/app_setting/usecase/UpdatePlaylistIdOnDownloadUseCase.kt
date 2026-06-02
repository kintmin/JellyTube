package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository

class UpdatePlaylistIdOnDownloadUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(playlistId: Int): Result<Unit> {
        return appSettingRepository.updatePlaylistIdOnDownload(playlistId)
    }
}
