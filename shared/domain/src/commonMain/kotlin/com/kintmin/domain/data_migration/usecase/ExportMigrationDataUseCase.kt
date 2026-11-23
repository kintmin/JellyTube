package com.kintmin.domain.data_migration.usecase

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import kotlinx.coroutines.flow.first

class ExportMigrationDataUseCase constructor(
    private val dataMigrationRepository: DataMigrationRepository,
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(scope: MigrationScope): Result<MigrationExport> = runCatching {
        val export = dataMigrationRepository.exportData(scope).getOrThrow()
        // 설정은 기기를 옮길 때만 따라간다. 곡 몇 개를 주면서 받는 기기의 기본 다운로드 자리를 바꾸지 않는다.
        if (scope !is MigrationScope.Everything) return@runCatching export
        export.copy(
            downloadSettings = MigrationDownloadSettings(
                shouldInsertAtTop = appSettingRepository.getShouldInsertAtTopOnDownloadFlow().first(),
                playlistId = appSettingRepository.getPlaylistIdOnDownloadFlow().first(),
            ),
        )
    }
}
