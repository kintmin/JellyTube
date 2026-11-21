package com.kintmin.presentation.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressHintTest {

    @Test
    fun `빠르게 눌러 오래 머문 뒤 놓는다`() {
        assertEquals(0f, pressAt(0f))
        assertTrue(pressAt(0.05f) in 0f..1f)
        listOf(0.2f, 0.4f, 0.65f).forEach { assertEquals(1f, pressAt(it)) }
        assertTrue(pressAt(0.8f) in 0f..1f)
        listOf(0.9f, 1f).forEach { assertEquals(0f, pressAt(it)) }
    }
}
