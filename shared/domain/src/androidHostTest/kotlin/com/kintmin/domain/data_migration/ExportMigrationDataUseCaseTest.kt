package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportMigrationDataUseCaseTest {

    @Test
    fun `내보낼 때 다운로드 설정 두 개를 함께 싣는다`() = runTest {
        val repository = mockk<DataMigrationRepository> {
            coEvery { exportData() } returns Result.success(
                MigrationExport(
                    snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                    files = emptyList(),
                    downloadSettings = null,
                ),
            )
        }
        val appSettingRepository = mockk<AppSettingRepository> {
            every { getShouldInsertAtTopOnDownloadFlow() } returns flowOf(true)
            every { getPlaylistIdOnDownloadFlow() } returns flowOf(14)
        }

        val export = ExportMigrationDataUseCase(repository, appSettingRepository)().getOrThrow()

        assertEquals(MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 14), export.downloadSettings)
    }
}
