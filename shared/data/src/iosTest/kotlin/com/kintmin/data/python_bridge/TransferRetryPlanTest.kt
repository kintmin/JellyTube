package com.kintmin.data.python_bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransferRetryPlanTest {

    @Test
    fun `5xx 와 429 만 같은 주소로 다시 받는다`() {
        listOf(429, 500, 503, 599).forEach { assertTrue(TransferRetryPlan.isTransient(it), "$it") }
        listOf(200, 206, 400, 403, 404, 410, 600).forEach { assertFalse(TransferRetryPlan.isTransient(it), "$it") }
    }

    @Test
    fun `재시도는 두 번까지 간격을 늘려 가며 한다`() {
        assertEquals(1.0, TransferRetryPlan.delaySeconds(1))
        assertEquals(3.0, TransferRetryPlan.delaySeconds(2))
        assertNull(TransferRetryPlan.delaySeconds(3))
        assertNull(TransferRetryPlan.delaySeconds(0))
    }
}
