package com.kintmin.presentation.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class TopAppBarStyleTest {

    // 배율 1.0 에서 1sp 와 1dp 가 같으므로 값으로 견준다.
    private val lowestBar = minOf(topAppBarHeight.value, topAppBarCompactHeight.value)

    @Test
    fun `제목 라인 높이는 글자보다 커야 한다`() {
        assertTrue(topAppBarRootTitleLineHeight.value > topAppBarRootTitleFontSize.value)
        assertTrue(topAppBarDetailTitleLineHeight.value > topAppBarDetailTitleFontSize.value)
    }

    @Test
    fun `제목 라인 높이는 글꼴 배율이 두 배가 되어도 가장 낮은 상단바를 넘지 않는다`() {
        assertTrue(topAppBarRootTitleLineHeight.value * 2 <= lowestBar)
        assertTrue(topAppBarDetailTitleLineHeight.value * 2 <= lowestBar)
    }

    @Test
    fun `아이콘은 가장 낮은 상단바 안에 들어간다`() {
        assertTrue(topAppBarIconSize.value <= lowestBar)
    }
}
