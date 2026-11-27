package com.kintmin.domain.app_setting.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 첫 화면 안내를 이미 보여 줬는지. 한 번 보여 준 것은 다시 띄우지 않는다. */
class FetchHomeNoticeShownUseCase constructor(
    private val appSettingRepository: AppSettingRepository,
) {
    operator fun invoke(): Flow<HomeNoticeShown> = combine(
        appSettingRepository.getHasShownNotificationNoticeFlow(),
        appSettingRepository.getHasShownBackupNoticeFlow(),
    ) { notification, backup ->
        HomeNoticeShown(notification = notification, backup = backup)
    }
}

data class HomeNoticeShown(
    val notification: Boolean,
    val backup: Boolean,
)
