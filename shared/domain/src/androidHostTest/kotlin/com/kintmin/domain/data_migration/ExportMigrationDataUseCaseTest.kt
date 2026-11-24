package com.kintmin.domain.data_migration

import com.kintmin.domain.app_setting.repository.AppSettingRepository
import com.kintmin.domain.data_migration.model.MigrationDownloadSettings
import com.kintmin.domain.data_migration.model.MigrationExport
import com.kintmin.domain.data_migration.model.MigrationScope
import com.kintmin.domain.data_migration.model.MigrationSnapshot
import com.kintmin.domain.data_migration.repository.DataMigrationRepository
import com.kintmin.domain.data_migration.usecase.ExportMigrationDataUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportMigrationDataUseCaseTest {

    private val repository = mockk<DataMigrationRepository> {
        coEvery { exportData(any()) } returns Result.success(
            MigrationExport(
                snapshot = MigrationSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
                files = emptyList(),
                downloadSettings = null,
            ),
        )
    }
    private val appSettingRepository = mockk<AppSettingRepository> {
        every { getShouldInsertAtTopOnDownloadFlow() } returns flowOf(true)
        every { getPlaylistIdOnDownloadFlow() } returns flowOf(14)
    }

    @Test
    fun `동기화에는 다운로드 설정 두 개를 함께 싣는다`() = runTest {
        val scope = MigrationScope.Everything(includesSecret = false)

        val export = ExportMigrationDataUseCase(repository, appSettingRepository)(scope).getOrThrow()

        assertEquals(MigrationDownloadSettings(shouldInsertAtTop = true, playlistId = 14), export.downloadSettings)
    }

    @Test
    fun `곡이나 플레이리스트만 공유할 때는 설정을 싣지 않는다`() = runTest {
        val scope = MigrationScope.AudioMedias(setOf(101))

        val export = ExportMigrationDataUseCase(repository, appSettingRepository)(scope).getOrThrow()

        assertNull(export.downloadSettings)
    }
}
