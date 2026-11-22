package com.kintmin.desktop.update

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindDesktopUpdateTest {

    private val release = DesktopRelease(versionName = "1.1.0", downloadUrl = "https://dl/msi")

    @Test
    fun `새 버전이 있으면 알린다`() = runTest {
        assertEquals(release, findDesktopUpdate(installedVersion = "1.0.1", ignoredVersion = null) { release })
    }

    @Test
    fun `설치된 버전을 모르면 확인하지 않는다`() = runTest {
        var fetched = false
        val update = findDesktopUpdate(installedVersion = null, ignoredVersion = null) { fetched = true; release }

        assertNull(update)
        assertEquals(false, fetched)
    }

    @Test
    fun `최신이면 알리지 않는다`() = runTest {
        assertNull(findDesktopUpdate(installedVersion = "1.1.0", ignoredVersion = null) { release })
    }

    @Test
    fun `무시한 버전은 알리지 않는다`() = runTest {
        assertNull(findDesktopUpdate(installedVersion = "1.0.1", ignoredVersion = "1.1.0") { release })
    }

    @Test
    fun `무시한 뒤 더 새 버전이 나오면 다시 알린다`() = runTest {
        assertEquals(release, findDesktopUpdate(installedVersion = "1.0.1", ignoredVersion = "1.0.5") { release })
    }

    @Test
    fun `확인에 실패하면 알리지 않는다`() = runTest {
        assertNull(findDesktopUpdate(installedVersion = "1.0.1", ignoredVersion = null) { null })
    }
}
