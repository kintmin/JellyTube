package com.kintmin.jellytube.worker

import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyResetImmediatelyWorkerTest {

    @Test
    fun `전날 마지막 걸음을 저장하지 못하면 다시 시도하고 세 번째부터는 실패로 끝낸다`() {
        val failed = Result.failure<Unit>(Exception("db"))

        assertEquals(ListenableWorker.Result.retry(), dailyResetWorkResult(failed, runAttemptCount = 0))
        assertEquals(ListenableWorker.Result.failure(), dailyResetWorkResult(failed, runAttemptCount = 3))
        assertEquals(ListenableWorker.Result.success(), dailyResetWorkResult(Result.success(Unit), runAttemptCount = 0))
    }
}
