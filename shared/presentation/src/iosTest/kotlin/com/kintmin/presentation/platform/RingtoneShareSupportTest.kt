package com.kintmin.presentation.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingtoneShareSupportTest {

    @Test
    fun `26 이상은 지원한다`() {
        assertTrue(isRingtoneShareSupported("26.0"))
        assertTrue(isRingtoneShareSupported("27.2.1"))
    }

    @Test
    fun `26 미만은 지원하지 않는다`() {
        assertFalse(isRingtoneShareSupported("18.6.2"))
        assertFalse(isRingtoneShareSupported("25.9"))
    }

    @Test
    fun `읽을 수 없는 버전은 지원하지 않는다`() {
        assertFalse(isRingtoneShareSupported(""))
        assertFalse(isRingtoneShareSupported("abc"))
    }
}
