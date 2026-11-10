package com.kintmin.data.local_file

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class AppLogFileTest {

    private val fileManager = FileManagerImpl()

    @BeforeTest
    fun clearLogDirectory() {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )?.path ?: return

        NSFileManager.defaultManager.removeItemAtPath("$documents/app_logs", error = null)
    }

    @Test
    fun `최신 줄이 위로 온다`() = runTest {
        fileManager.appendAppLog("2026-01-01", "첫 줄")
        fileManager.appendAppLog("2026-01-01", "둘째 줄")

        assertEquals(
            listOf("둘째 줄", "첫 줄"),
            fileManager.fetchAppLogLineList("2026-01-01").getOrThrow(),
        )
    }

    @Test
    fun `날짜 목록은 최신이 먼저다`() = runTest {
        fileManager.appendAppLog("2026-01-01", "줄")
        fileManager.appendAppLog("2026-01-03", "줄")
        fileManager.appendAppLog("2026-01-02", "줄")

        assertEquals(
            listOf("2026-01-03", "2026-01-02", "2026-01-01"),
            fileManager.fetchAppLogDateList().getOrThrow(),
        )
    }

    @Test
    fun `보존 개수를 넘으면 오래된 날짜부터 지운다`() = runTest {
        repeat(MAX_LOG_FILE_COUNT + 3) { index ->
            fileManager.appendAppLog("2026-02-${(index + 1).toString().padStart(2, '0')}", "줄")
        }

        val dates = fileManager.fetchAppLogDateList().getOrThrow()

        assertEquals(MAX_LOG_FILE_COUNT, dates.size)
        assertEquals("2026-02-${MAX_LOG_FILE_COUNT + 3}", dates.first())
        assertEquals("2026-02-04", dates.last())
    }

    @Test
    fun `없는 날짜는 빈 목록이다`() = runTest {
        assertEquals(emptyList(), fileManager.fetchAppLogLineList("2026-12-31").getOrThrow())
    }
}
