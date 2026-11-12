package com.kintmin.presentation.ui.player_bar

import com.kintmin.presentation.port.MediaControllerManager
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class PlayerBarViewModelTest {

    private val media = MediaControlData(
        mediaId = "7",
        mediaFileUri = "/audios/7.m4a",
        mediaTitle = "제목",
        mediaDescription = "",
        mediaArtist = "",
        mediaDurationMs = 51_000L,
        mediaArtworkFileUri = null,
    )

    private var playing: MediaControlData? = null
    private var isPlaying = false

    private val manager = mockk<MediaControllerManager> {
        every { playingMediaItem } answers { playing }
        every { isPlaying } answers { this@PlayerBarViewModelTest.isPlaying }
        every { currentPosition } returns 3_000L
    }

    @Test
    fun `재생이 시작된 뒤 한 번의 갱신으로 곡 정보와 재생 상태를 함께 채운다`() {
        val viewModel = PlayerBarViewModel(manager)
        playing = media
        isPlaying = true

        viewModel.sendIntent(PlayerBarIntent.OnRefreshMediaData)

        val state = viewModel.currentMediaItem.value
        assertEquals("7", state.id)
        assertTrue(state.isPlaying)
        assertEquals(3_000L.milliseconds, state.currentDuration)
    }
}
