package com.kintmin.presentation.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupFolderLabelTest {

    @Test
    fun `저장소 이름을 떼고 폴더 경로만 보여 준다`() {
        assertEquals("Download/JellyTube", treeFolderLabelOf("primary:Download/JellyTube"))
        assertEquals("Backup", treeFolderLabelOf("1A2B-3C4D:Backup"))
    }

    @Test
    fun `콜론이 없거나 뒤가 비면 받은 값을 그대로 보여 준다`() {
        assertEquals("primary", treeFolderLabelOf("primary"))
        assertEquals("primary:", treeFolderLabelOf("primary:"))
    }
}
