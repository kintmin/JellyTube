package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import kotlinx.coroutines.flow.first

class ExportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(): Result<MigrationExport> = runCatching {
        val export = dataMigrationRepository.exportData().getOrThrow()
        export.copy(
            downloadSettings = MigrationDownloadSettings(
                shouldInsertAtTop = appSettingRepository.getShouldInsertAtTopOnDownloadFlow().first(),
                playlistId = appSettingRepository.getPlaylistIdOnDownloadFlow().first(),
            ),
        )
    }
}
