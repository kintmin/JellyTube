package com.kintmin.presentation.platform

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosToastHostTest {

    @AfterTest
    fun tearDown() {
        ToastHost.current.value?.let { ToastHost.clear(it.id) }
    }

    @Test
    fun `겹치면 마지막 것이 이긴다`() {
        IosToaster.show("첫 번째")
        IosToaster.show("두 번째")

        assertEquals("두 번째", ToastHost.current.value?.text)
    }

    @Test
    fun `그 사이 새 메시지가 올라왔으면 지우지 않는다`() {
        IosToaster.show("첫 번째")
        val firstId = requireNotNull(ToastHost.current.value).id
        IosToaster.show("두 번째")

        ToastHost.clear(firstId)

        assertEquals("두 번째", ToastHost.current.value?.text)
    }

    @Test
    fun `자기 메시지는 지운다`() {
        IosToaster.show("메시지")

        ToastHost.clear(requireNotNull(ToastHost.current.value).id)

        assertNull(ToastHost.current.value)
    }

    @Test
    fun `show 와 showLong 의 노출 길이가 다르다`() {
        IosToaster.show("짧게")
        assertEquals(SHORT_DURATION_MILLIS, ToastHost.current.value?.durationMillis)

        IosToaster.showLong("길게")
        assertEquals(LONG_DURATION_MILLIS, ToastHost.current.value?.durationMillis)
    }
}
