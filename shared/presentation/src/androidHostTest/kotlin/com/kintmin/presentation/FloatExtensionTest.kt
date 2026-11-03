package com.kintmin.presentation

import com.kintmin.presentation.extension.toFixed
import org.junit.Assert.assertEquals
import org.junit.Test

class FloatExtensionTest {

    @Test
    fun `toFixed 는 자리수를 채워 표기한다`() {
        assertEquals("1.0", 1.0f.toFixed(1))
        assertEquals("1.00", 1.0f.toFixed(2))
        assertEquals("0.50", 0.5f.toFixed(2))
        assertEquals("3.00", 3.0f.toFixed(2))
    }

    @Test
    fun `toFixed 는 반올림한다`() {
        assertEquals("1.3", 1.25f.toFixed(1))
        assertEquals("1.25", 1.25f.toFixed(2))
        assertEquals("1", 1.4f.toFixed(0))
        assertEquals("2", 1.5f.toFixed(0))
    }

    @Test
    fun `toFixed 는 음수의 부호를 유지한다`() {
        assertEquals("-1.50", (-1.5f).toFixed(2))
        assertEquals("-0.25", (-0.25f).toFixed(2))
    }

    @Test
    fun `재생배속 표기 규칙을 만족한다`() {
        // 정수 배속은 소수 1자리, 그 외는 2자리에서 뒤쪽 0 을 떼어 쓴다.
        assertEquals("1.0", 1.0f.toFixed(1))
        assertEquals("1.2", 1.2f.toFixed(2).trimEnd('0'))
        assertEquals("1.25", 1.25f.toFixed(2).trimEnd('0'))
        assertEquals("0.25", 0.25f.toFixed(2).trimEnd('0'))
    }
}
