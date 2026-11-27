package com.kintmin.jellytube.backup

import com.kintmin.data.backup.BackupFiles
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    /** 백업 규칙이 가르는 영역 전부. 하나라도 빠지면 그 영역이 클라우드로 올라간다. */
    private val everyDomainExcluded = listOf(
        "root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref",
    ).map { it to "." }

    private val nothing = emptyList<Pair<String, String>>()

    @Test
    fun `Android 11 이하 클라우드 백업은 아무것도 싣지 않는다`() {
        assertEquals(nothing, rules("backup_rules.xml", "include"))
        assertEquals(everyDomainExcluded, rules("backup_rules.xml", "exclude"))
    }

    @Test
    fun `Android 12 이상 클라우드 백업은 아무것도 싣지 않는다`() {
        assertEquals(nothing, rules("data_extraction_rules.xml", "include", section = "cloud-backup"))
        assertEquals(everyDomainExcluded, rules("data_extraction_rules.xml", "exclude", section = "cloud-backup"))
    }

    @Test
    fun `기기 간 이전은 설치 식별만 빼고 전부 옮긴다`() {
        // include 를 하나라도 쓰면 적지 않은 것이 전부 빠진다.
        assertEquals(nothing, rules("data_extraction_rules.xml", "include", section = "device-transfer"))
        assertEquals(
            listOf("file" to "${BackupFiles.INSTALL_ID_DIR_NAME}/"),
            rules("data_extraction_rules.xml", "exclude", section = "device-transfer"),
        )
    }

    private fun rules(fileName: String, tag: String, section: String? = null): List<Pair<String, String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/$fileName"))
        val scope = section?.let { document.getElementsByTagName(it).item(0) as Element } ?: document.documentElement
        val nodes = scope.getElementsByTagName(tag)
        return (0 until nodes.length).map { index ->
            val rule = nodes.item(index) as Element
            rule.getAttribute("domain") to rule.getAttribute("path")
        }
    }
}
