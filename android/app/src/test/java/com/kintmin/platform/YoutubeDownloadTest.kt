package com.kintmin.platform

import com.kintmin.platform.worker.YoutubeDownloadWorker
import io.mockk.mockk
import org.junit.Test

class YoutubeDownloadTest {
    @Test
    fun `유튜브 워커 mock 생성 테스트`() {
        val worker = mockk<YoutubeDownloadWorker>(relaxed = true)
        assert(worker != null)
    }
}
