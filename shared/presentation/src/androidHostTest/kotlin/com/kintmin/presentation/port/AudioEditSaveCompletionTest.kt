package com.kintmin.presentation.port

import com.kintmin.domain.audio_media.model.AudioMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AudioEditSaveCompletionTest {

    private val mediaControllerManager: MediaControllerManager = mockk {
        every { tryReplaceMediaItem(any(), any()) } returns Result.success(Unit)
    }

    private val audioMedia = AudioMedia(
        id = 7,
        source = "https://example.com/origin",
        name = "곡",
        artist = "가수",
        description = "",
        audioDuration = 300.seconds,
        audioFileFullPath = "/audio/origin.m4a",
        imageFileFullPath = null,
        editedAudioFileFullPath = "/audio/edited.m4a",
        editedAudioDuration = 120.seconds,
        createdTime = LocalDateTime(2026, 11, 13, 0, 0),
    )

    @Test
    fun `덮어쓰면 다시 재생해도 갱신되지 않는 재생 큐의 항목을 편집본으로 바꾼다`() {
        val notice = mediaControllerManager.completeAudioEditSave(audioMedia, isOverwrite = true)

        assertEquals(WorkResultNotice.AudioEditSave.OverwriteSuccess("곡", 7), notice)
        verify(exactly = 1) {
            mediaControllerManager.tryReplaceMediaItem(
                mediaId = 7,
                mediaItem = match { it.mediaFileUri == "/audio/edited.m4a" && it.mediaDurationMs == 120_000L },
            )
        }
    }

    @Test
    fun `새 음원으로 저장하면 재생 큐를 건드리지 않는다`() {
        val notice = mediaControllerManager.completeAudioEditSave(audioMedia, isOverwrite = false)

        assertEquals(WorkResultNotice.AudioEditSave.NewMediaSuccess("곡", 7), notice)
        verify(exactly = 0) { mediaControllerManager.tryReplaceMediaItem(any(), any()) }
    }
}
