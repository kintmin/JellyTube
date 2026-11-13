package com.kintmin.data.local_file

import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileManagerCoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val root: Path by lazy { tempFolder.root.toOkioPath() }
    private val core by lazy {
        FileManagerCore(
            object : AppDirectories {
                override fun audio() = root / "audio"
                override fun image() = root / "image"
                override fun lyric() = root / AppDirectories.LYRIC_DIR_NAME
                override fun log() = root / AppDirectories.LOG_DIR_NAME
                override fun migrationStaging() = root / AppDirectories.MIGRATION_STAGING_DIR_NAME
            },
        )
    }

    @Test
    fun `로그는 최신 줄이 위로 오고 날짜는 최신순이다`() = runTest {
        core.appendAppLog("2026-01-01", "첫 줄")
        core.appendAppLog("2026-01-01", "둘째 줄")
        core.appendAppLog("2026-01-02", "다음 날")

        assertEquals(listOf("둘째 줄", "첫 줄"), core.fetchAppLogLineList("2026-01-01").getOrThrow())
        assertEquals(listOf("2026-01-02", "2026-01-01"), core.fetchAppLogDateList().getOrThrow())
        assertEquals(emptyList<String>(), core.fetchAppLogLineList("2025-12-31").getOrThrow())
    }

    @Test
    fun `로그 파일은 보관 개수를 넘으면 오래된 것부터 지운다`() = runTest {
        (1..MAX_LOG_FILE_COUNT + 2).forEach { day -> core.appendAppLog("2026-01-${day.toString().padStart(2, '0')}", "줄") }

        val dates = core.fetchAppLogDateList().getOrThrow()
        assertEquals(MAX_LOG_FILE_COUNT, dates.size)
        assertEquals("2026-01-16", dates.first())
        assertEquals(dates.size, core.fetchAppLogFileFullPaths().getOrThrow().size)
    }

    @Test
    fun `가사는 싱크 여부로 확장자를 정하고 다시 읽는다`() = runTest {
        assertEquals("lrc", core.saveLyrics("[00:01.00]가사", "a", synced = true).getOrThrow())
        assertEquals("txt", core.saveLyrics("가사", "b", synced = false).getOrThrow())

        assertEquals("[00:01.00]가사", core.fetchLyrics("a.lrc").getOrThrow())
        assertTrue(core.fetchLyrics("없음.lrc").isFailure)
    }

    @Test
    fun `음원과 이미지 디렉터리의 파일만 모으고 지운다`() = runTest {
        val audio = core.getAudioFileFullPath("a.m4a").getOrThrow()
        val image = core.getImageFileFullPath("a.webp").getOrThrow()
        java.io.File(audio).writeText("a")
        java.io.File(image).writeText("i")
        java.io.File(core.audioDir().toString(), "sub").mkdirs()

        assertEquals(setOf(audio, image), core.listAudioAndImageFileFullPaths().getOrThrow().toSet())

        core.deleteFileAtFullPath(audio).getOrThrow()
        core.deleteFileAtFullPath(audio).getOrThrow()
        assertEquals(listOf(image), core.listAudioAndImageFileFullPaths().getOrThrow())
    }
}
