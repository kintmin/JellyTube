package com.kintmin.presentation.ui.setting.backup_restore

import com.kintmin.domain.backup.model.BackupFailureReason
import com.kintmin.domain.backup.model.BackupFolder
import com.kintmin.domain.backup.model.BackupStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStatusTextTest {

    private val folder = BackupFolder("content://tree/1", "Download/JellyTube")

    @Test
    fun `폴더를 고르지 않았으면 설정 타일에서 알아야 하는 상태다`() {
        assertTrue(BackupStatus(null, null, null, null).isWarning)
    }

    @Test
    fun `마지막 백업이 실패했으면 알아야 하고 그 뒤에 성공했으면 아니다`() {
        assertTrue(BackupStatus(folder, 1_000, 2_000, BackupFailureReason.FOLDER_UNAVAILABLE).isWarning)
        assertFalse(BackupStatus(folder, 2_000, 1_000, BackupFailureReason.FOLDER_UNAVAILABLE).isWarning)
    }

    @Test
    fun `폴더가 있고 실패 기록이 없으면 알릴 것이 없다`() {
        assertFalse(BackupStatus(folder, 1_000, null, null).isWarning)
        assertFalse(BackupStatus(folder, null, null, null).isWarning)
    }
}
