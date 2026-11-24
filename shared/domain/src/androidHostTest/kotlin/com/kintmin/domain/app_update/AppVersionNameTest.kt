package com.kintmin.domain.app_update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionNameTest {

    @Test
    fun `자리마다 숫자로 비교한다`() {
        assertTrue(AppVersionName.isNewer("2.0.1", "2.0.0"))
        assertTrue(AppVersionName.isNewer("2.10.0", "2.9.9"))
        assertFalse(AppVersionName.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun `같은 버전은 새 버전이 아니다`() {
        assertFalse(AppVersionName.isNewer("2.0.0", "2.0.0"))
    }

    @Test
    fun `빠진 자리는 0 으로 본다`() {
        assertFalse(AppVersionName.isNewer("2.0", "2.0.0"))
        assertTrue(AppVersionName.isNewer("2.0.0.1", "2.0"))
    }

    @Test
    fun `개발 빌드 접미사는 무시한다`() {
        assertTrue(AppVersionName.isNewer("2.0.1", "2.0.0-dev"))
        assertFalse(AppVersionName.isNewer("2.0.0", "2.0.0-dev"))
    }
}
