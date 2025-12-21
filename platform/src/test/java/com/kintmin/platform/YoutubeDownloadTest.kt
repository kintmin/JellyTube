package com.kintmin.platform

import com.kintmin.platform.worker.YoutubeDownloadWorker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.mock

class YoutubeDownloadTest {
    @Before
    fun setUp() {

    }

    @After
    fun tearDown() {

    }

    @Test
    fun `유튜브 다운로드 테스트`() = runTest {
        val worker = mock(YoutubeDownloadWorker::class.java, CALLS_REAL_METHODS)


    }
}