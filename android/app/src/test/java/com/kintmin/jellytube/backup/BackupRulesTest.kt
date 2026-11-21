package com.kintmin.jellytube.backup

import com.kintmin.data.backup.BackupFiles
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    private val expected = listOf("file" to "${BackupFiles.LOCAL_DIR_NAME}/")

    private fun includes(fileName: String, section: String? = null): List<Pair<String, String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/$fileName"))
        val scope = section?.let { document.getElementsByTagName(it).item(0) as Element } ?: document.documentElement
        val nodes = scope.getElementsByTagName("include")
        return (0 until nodes.length).map { index ->
            val include = nodes.item(index) as Element
            include.getAttribute("domain") to include.getAttribute("path")
        }
    }

    @Test
    fun `Android 11 이하 규칙은 백업 사본 디렉터리만 싣는다`() {
        assertEquals(expected, includes("backup_rules.xml"))
    }

    @Test
    fun `Android 12 이상 클라우드 백업은 백업 사본 디렉터리만 싣는다`() {
        assertEquals(expected, includes("data_extraction_rules.xml", section = "cloud-backup"))
    }
}
