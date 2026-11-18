package com.kintmin.ios.task

import com.kintmin.presentation.port.model.RunningTask
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosRunningTaskTrackerTest {

    @Test
    fun `작업이 도는 동안만 센다`() = runTest {
        IosRunningTaskTracker.track(RunningTask.LYRICS_VARIANT) {
            IosRunningTaskTracker.track(RunningTask.LYRICS_VARIANT) {
                assertEquals(2, IosRunningTaskTracker.snapshot()[RunningTask.LYRICS_VARIANT])
            }
            assertEquals(1, IosRunningTaskTracker.snapshot()[RunningTask.LYRICS_VARIANT])
        }

        assertEquals(0, IosRunningTaskTracker.snapshot()[RunningTask.LYRICS_VARIANT] ?: 0)
    }

    @Test
    fun `작업이 실패해도 센 것을 되돌린다`() = runTest {
        runCatching {
            IosRunningTaskTracker.track(RunningTask.YOUTUBE_DOWNLOAD) { error("실패") }
        }

        assertTrue((IosRunningTaskTracker.snapshot()[RunningTask.YOUTUBE_DOWNLOAD] ?: 0) == 0)
    }
}
