package com.kintmin.presentation.ui.player_bar

import com.kintmin.presentation.port.PlaybackController
import com.kintmin.presentation.port.model.MediaControlData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private var playlistId: Int? = 1

    private val manager = mockk<PlaybackController> {
        every { playingMediaItem } answers { playing }
        every { isPlaying } answers { this@PlayerBarViewModelTest.isPlaying }
        every { currentPosition } returns 3_000L
        every { currentPlaylistId } answers { playlistId }
    }

    @Test
    fun `재생이 시작된 뒤 한 번의 갱신으로 곡 정보와 재생 상태를 함께 채운다`() {
        val viewModel = PlayerBarViewModel(manager)
        playing = media
        isPlaying = true

        viewModel.sendIntent(PlayerBarIntent.OnRefreshMediaData)

        val state = viewModel.uiState.value
        assertEquals("7", state.id)
        assertTrue(state.isPlaying)
        assertEquals(3_000L.milliseconds, state.currentDuration)
    }

    @Test
    fun `같은 곡이 편집돼 길이가 바뀌면 재생바 길이도 바뀐다`() {
        playing = media
        isPlaying = true
        val viewModel = PlayerBarViewModel(manager)
        playing = media.copy(mediaDurationMs = 40_000L)

        viewModel.sendIntent(PlayerBarIntent.OnRefreshMediaData)

        assertEquals(40_000L.milliseconds, viewModel.uiState.value.playbackDuration)
    }

    @Test
    fun `같은 곡이라도 다른 플레이리스트에서 듣고 있으면 이 플레이리스트에서는 재생 중이 아니다`() {
        playing = media
        val viewModel = PlayerBarViewModel(manager)
        assertEquals(7, viewModel.uiState.value.nowPlayingMediaIdIn(1))

        playlistId = 2
        viewModel.sendIntent(PlayerBarIntent.OnRefreshMediaData)

        assertNull(viewModel.uiState.value.nowPlayingMediaIdIn(1))
        assertEquals(7, viewModel.uiState.value.nowPlayingMediaIdIn(2))
    }

    @Test
    fun `걸린 곡이 없으면 듣고 있는 플레이리스트도 없다`() {
        playing = null
        val viewModel = PlayerBarViewModel(manager)

        assertNull(viewModel.uiState.value.playlistId)
    }
}
