package com.kintmin.data.backup

import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class BackupFileNameTest {

    private val installId = "9f1c2d3e-4a5b-6c7d-8e9f-0a1b2c3d4e5f"
    private val otherInstallId = "11112222-3333-4444-5555-666677778888"
    private val seoul = TimeZone.of("Asia/Seoul")

    private fun millisOf(isoInstant: String) = Instant.parse(isoInstant).toEpochMilliseconds()

    @Test
    fun `날짜가 앞에 오고 설치 값이 뒤에 붙는다`() {
        val name = BackupFileName.build(millisOf("2026-11-27T00:30:00Z"), TimeZone.UTC, installId)

        assertEquals("JellyTube_20261127_$installId.json.gz", name)
    }

    @Test
    fun `날짜는 넘긴 시간대로 가른다`() {
        val millis = millisOf("2026-11-27T15:00:00Z")

        assertEquals("20261128", BackupFileName.parse(BackupFileName.build(millis, seoul, installId))?.date)
        assertEquals("20261127", BackupFileName.parse(BackupFileName.build(millis, TimeZone.UTC, installId))?.date)
    }

    @Test
    fun `우리가 쓴 이름에서 날짜와 설치 값을 되읽는다`() {
        val parsed = BackupFileName.parse("JellyTube_20261127_$installId.json.gz")

        assertEquals("20261127", parsed?.date)
        assertEquals(installId, parsed?.installId)
    }

    @Test
    fun `우리 이름이 아니면 읽지 않는다`() {
        assertNull(BackupFileName.parse("JellyTube_20261127.json.gz"))
        assertNull(BackupFileName.parse("JellyTube_2026112_$installId.json.gz"))
        assertNull(BackupFileName.parse("JellyTube_20261127_.json.gz"))
        assertNull(BackupFileName.parse("other_20261127_$installId.json.gz"))
        assertNull(BackupFileName.parse("JellyTube_20261127_$installId.json"))
    }

    @Test
    fun `옛 고정 이름과 제공자가 번호를 붙여 밀어낸 이름을 모두 백업으로 본다`() {
        assertEquals(0, BackupFileName.legacyOrderOf("jellytube-backup.json.gz"))
        assertEquals(1, BackupFileName.legacyOrderOf("jellytube-backup.json.gz (1)"))
        assertEquals(12, BackupFileName.legacyOrderOf("jellytube-backup.json.gz (12)"))
        assertEquals(1, BackupFileName.legacyOrderOf("jellytube-backup (1).json.gz"))
    }

    @Test
    fun `같은 폴더의 다른 파일은 백업으로 보지 않는다`() {
        assertNull(BackupFileName.legacyOrderOf("jellytube-backup.json.gz (copy)"))
        assertNull(BackupFileName.legacyOrderOf("jellytube-backup.json.gz ()"))
        assertNull(BackupFileName.legacyOrderOf("jellytube-backup (copy).json.gz"))
        assertNull(BackupFileName.legacyOrderOf("jellytube-backup.json"))
        assertNull(BackupFileName.legacyOrderOf("jellytube-backup (1).json"))
        assertNull(BackupFileName.legacyOrderOf("other.json.gz"))
    }

    @Test
    fun `최근 날짜가 먼저 오고 날짜를 모르는 옛 이름이 뒤에 온다`() {
        val names = listOf(
            "jellytube-backup.json.gz",
            "JellyTube_20261120_$installId.json.gz",
            "노래.mp3",
            "jellytube-backup.json.gz (2)",
            "JellyTube_20261127_$otherInstallId.json.gz",
        )

        assertEquals(
            listOf(
                "JellyTube_20261127_$otherInstallId.json.gz",
                "JellyTube_20261120_$installId.json.gz",
                "jellytube-backup.json.gz (2)",
                "jellytube-backup.json.gz",
            ),
            BackupFileName.candidatesNewestFirst(names),
        )
    }

    @Test
    fun `같은 날짜면 설치 값으로 순서를 고정한다`() {
        val names = listOf(
            "JellyTube_20261127_$installId.json.gz",
            "JellyTube_20261127_$otherInstallId.json.gz",
        )

        // 플랫폼이 어느 순서로 돌려주든 같은 답이어야 한다.
        assertEquals(
            BackupFileName.candidatesNewestFirst(names),
            BackupFileName.candidatesNewestFirst(names.reversed()),
        )
    }

    @Test
    fun `백업이 아닌 파일만 있으면 후보가 없다`() {
        assertEquals(emptyList<String>(), BackupFileName.candidatesNewestFirst(listOf("노래.mp3", "표지.png")))
    }
}
