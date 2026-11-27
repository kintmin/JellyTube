package com.kintmin.domain.audio_play_setting

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_play_setting.usecase.FetchSleepTimerAnnouncementOptionsFlowUseCase
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FetchSleepTimerAnnouncementOptionsFlowUseCaseTest {

    private val useCase = FetchSleepTimerAnnouncementOptionsFlowUseCase(mockk(), mockk())

    private fun audioMedia(id: Int, audioDuration: Duration?) = AudioMedia(
        id = id,
        source = "source",
        name = "name$id",
        artist = "artist",
        description = "",
        audioDuration = audioDuration,
        audioFileFullPath = "/audio$id.m4a",
        imageFileFullPath = null,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )

    private val shortMedia = audioMedia(id = 1, audioDuration = 10.seconds)
    private val longMedia = audioMedia(id = 2, audioDuration = 5.seconds * 60)
    private val unknownLengthMedia = audioMedia(id = 3, audioDuration = null)

    @Test
    fun `길이를 아는 음원만 고를 수 있게 남는다`() {
        val options = useCase.resolve(
            savedId = null,
            mediaList = listOf(shortMedia, longMedia, unknownLengthMedia),
        )

        assertEquals(listOf(shortMedia, longMedia), options.selectable)
    }

    @Test
    fun `고른 음원이 후보에 있으면 그것이 선택이다`() {
        val options = useCase.resolve(savedId = 1, mediaList = listOf(shortMedia, longMedia))

        assertEquals(shortMedia, options.selected)
    }

    @Test
    fun `고르지 않았으면 선택이 없다`() {
        val options = useCase.resolve(savedId = null, mediaList = listOf(shortMedia))

        assertNull(options.selected)
    }

    @Test
    fun `고른 음원이 지워지면 선택이 없어진다`() {
        val options = useCase.resolve(savedId = 1, mediaList = listOf(longMedia))

        assertNull(options.selected)
    }

    @Test
    fun `고른 음원의 길이를 알 수 없게 되면 선택이 없어진다`() {
        val lengthLost = audioMedia(id = 1, audioDuration = null)

        val options = useCase.resolve(savedId = 1, mediaList = listOf(lengthLost))

        assertNull(options.selected)
    }
}
