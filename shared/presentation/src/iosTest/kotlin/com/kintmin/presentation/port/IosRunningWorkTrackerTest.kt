package com.kintmin.presentation.port

import com.kintmin.presentation.port.model.RunningWork
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosRunningWorkTrackerTest {

    @Test
    fun `작업이 도는 동안만 센다`() = runTest {
        IosRunningWorkTracker.track(RunningWork.LYRICS_VARIANT) {
            IosRunningWorkTracker.track(RunningWork.LYRICS_VARIANT) {
                assertEquals(2, IosRunningWorkTracker.snapshot()[RunningWork.LYRICS_VARIANT])
            }
            assertEquals(1, IosRunningWorkTracker.snapshot()[RunningWork.LYRICS_VARIANT])
        }

        assertEquals(0, IosRunningWorkTracker.snapshot()[RunningWork.LYRICS_VARIANT] ?: 0)
    }

    @Test
    fun `작업이 실패해도 센 것을 되돌린다`() = runTest {
        runCatching {
            IosRunningWorkTracker.track(RunningWork.YOUTUBE_DOWNLOAD) { error("실패") }
        }

        assertTrue((IosRunningWorkTracker.snapshot()[RunningWork.YOUTUBE_DOWNLOAD] ?: 0) == 0)
    }
}
