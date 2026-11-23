package com.kintmin.presentation.ui.custom_ui.zoomable

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomableViewTest {

    @Test
    fun `Horizontal 은 가로 Fit 배율을 사용한다`() {
        assertEquals(2f, effectiveMinScale(ZoomLimitMode.Horizontal, 2f, 3f))
    }

    @Test
    fun `Vertical 은 세로 Fit 배율을 사용한다`() {
        assertEquals(3f, effectiveMinScale(ZoomLimitMode.Vertical, 2f, 3f))
    }

    @Test
    fun `Auto 는 두 Fit 배율 중 작은 값을 사용한다`() {
        assertEquals(2f, effectiveMinScale(ZoomLimitMode.Auto, 2f, 3f))
        assertEquals(2f, effectiveMinScale(ZoomLimitMode.Auto, 3f, 2f))
    }
}
