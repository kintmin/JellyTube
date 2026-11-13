package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.model.SavedAudioEdit
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.SaveAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveAudioEditUseCaseTest {

    private val apply = mockk<ApplyAudioEditUseCase>()
    private val saveAsNew = mockk<SaveEditedAudioAsNewMediaUseCase>()
    private val useCase = SaveAudioEditUseCase(apply, saveAsNew)
    private val timeline = mockk<AudioEditTimeline> { every { toParts() } returns emptyList() }
    private val audioMedia = AudioMedia(
        id = 1,
        source = "s",
        name = "곡",
        artist = "",
        description = "",
        audioDuration = null,
        audioFileFullPath = "/a",
        imageFileFullPath = null,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )

    @Test
    fun `이름이 없으면 덮어쓴다`() = runTest {
        coEvery { apply(1, timeline) } returns Result.success(audioMedia)

        assertEquals(SavedAudioEdit.Overwritten(audioMedia), useCase(1, timeline, newMediaName = null).getOrThrow())
        coVerify(exactly = 0) { saveAsNew(any(), any(), any()) }
    }

    @Test
    fun `이름이 있으면 새 음원으로 저장한다`() = runTest {
        coEvery { saveAsNew(1, emptyList(), "새 이름") } returns Result.success(audioMedia)

        assertEquals(SavedAudioEdit.SavedAsNew(audioMedia), useCase(1, timeline, newMediaName = "새 이름").getOrThrow())
        coVerify(exactly = 0) { apply(any(), any()) }
    }
}
