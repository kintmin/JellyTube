package com.kintmin.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCopyNameTest {

    private val fileName = BackupFiles.FILE_NAME

    @Test
    fun `원래 이름과 MediaStore 가 번호를 붙여 밀어낸 이름을 모두 백업 사본으로 본다`() {
        assertTrue(isBackupCopyName("jellytube-backup.json.gz", fileName))
        assertTrue(isBackupCopyName("jellytube-backup.json.gz (1)", fileName))
        assertTrue(isBackupCopyName("jellytube-backup.json.gz (12)", fileName))
        assertTrue(isBackupCopyName("jellytube-backup (1).json.gz", fileName))
    }

    @Test
    fun `같은 폴더의 다른 파일은 백업 사본으로 보지 않는다`() {
        assertFalse(isBackupCopyName("jellytube-backup.json.gz (copy)", fileName))
        assertFalse(isBackupCopyName("jellytube-backup.json.gz ()", fileName))
        assertFalse(isBackupCopyName("jellytube-backup (copy).json.gz", fileName))
        assertFalse(isBackupCopyName("jellytube-backup.json", fileName))
        assertFalse(isBackupCopyName("jellytube-backup (1).json", fileName))
        assertFalse(isBackupCopyName("other.json.gz", fileName))
    }
}
