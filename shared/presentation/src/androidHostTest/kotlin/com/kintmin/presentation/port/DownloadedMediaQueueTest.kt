package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDateTime
import org.junit.Test

class DownloadedMediaQueueTest {

    private val manager = mockk<MediaControllerManager>(relaxed = true)
    private val audioMedia = AudioMedia(
        id = 7,
        source = "src",
        name = "곡",
        artist = "가수",
        description = "",
        audioDuration = null,
        audioFileFullPath = "/a.m4a",
        imageFileFullPath = null,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )

    private fun result(target: Int, top: Boolean) = DownloadedAudioMediaResult(
        audioMedia = audioMedia,
        playlistIdOnDownload = target,
        totalPlaylistId = 1,
        shouldInsertAtTopOnDownload = top,
    )

    @Test
    fun `전체와 대상 플레이리스트 둘 다에 넣는다`() {
        manager.addDownloadedMedia(result(target = 4, top = false))

        verify(exactly = 1) { manager.tryAddLastMediaItem(1, any()) }
        verify(exactly = 1) { manager.tryAddLastMediaItem(4, any()) }
    }

    @Test
    fun `대상이 전체와 같으면 한 번만 넣고 맨 위 설정을 따른다`() {
        manager.addDownloadedMedia(result(target = 1, top = true))

        verify(exactly = 1) { manager.tryAddFirstMediaItem(1, any()) }
        verify(exactly = 0) { manager.tryAddLastMediaItem(any(), any()) }
    }
}
