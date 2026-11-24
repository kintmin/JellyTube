package com.kintmin.desktop.update

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindOnReleasePagesTest {

    private fun mobile(n: Int) = List(n) { GitHubReleaseDto(tagName = "mobile/v2.$it.0", htmlUrl = "m$it") }
    private val desktop = GitHubReleaseDto(tagName = "desktop/v1.1.0", htmlUrl = "https://github.com/r/desktop")

    @Test
    fun `첫 쪽에 있으면 다음 쪽을 받지 않는다`() = runTest {
        val requested = mutableListOf<Int>()

        val release = findOnReleasePages("msi") { page ->
            requested += page
            mobile(RELEASES_PER_PAGE - 1) + desktop
        }

        assertEquals("1.1.0", release?.versionName)
        assertEquals(listOf(1), requested)
    }

    @Test
    fun `mobile 릴리스가 한 쪽을 넘게 쌓여도 desktop 이 나올 때까지 넘긴다`() = runTest {
        val pages = mapOf(1 to mobile(RELEASES_PER_PAGE), 2 to mobile(RELEASES_PER_PAGE), 3 to listOf(desktop))

        val release = findOnReleasePages("msi") { page -> pages.getValue(page) }

        assertEquals("1.1.0", release?.versionName)
    }

    @Test
    fun `마지막 쪽까지 없으면 멈춘다`() = runTest {
        val requested = mutableListOf<Int>()

        val release = findOnReleasePages("msi") { page ->
            requested += page
            if (page == 1) mobile(RELEASES_PER_PAGE) else mobile(3)
        }

        assertNull(release)
        assertEquals(listOf(1, 2), requested)
    }
}
