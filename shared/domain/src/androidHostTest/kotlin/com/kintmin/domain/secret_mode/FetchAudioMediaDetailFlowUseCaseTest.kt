package com.kintmin.domain.secret_mode

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaDetailFlowUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.secret_mode.usecase.FetchSecretModeEnabledFlowUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchAudioMediaDetailFlowUseCaseTest {

    private val isSecretModeEnabled = MutableStateFlow(false)
    private val repository: AudioTrackRepository = mockk {
        every { getPlaylistIdListFlow(AUDIO_MEDIA_ID) } returns flowOf(listOf(1, 5))
        every { getPlaylistTrackAggregateFlow(1, AUDIO_MEDIA_ID) } returns flowOf(aggregate(PlaylistType.TOTAL))
        every { getPlaylistTrackAggregateFlow(5, AUDIO_MEDIA_ID) } returns flowOf(aggregate(PlaylistType.SECRET))
    }
    private val useCase = FetchAudioMediaDetailFlowUseCase(
        repository,
        mockk<FetchSecretModeEnabledFlowUseCase> { every { this@mockk() } returns isSecretModeEnabled },
    )

    @Test
    fun `비밀모드가 아니면 곡이 속한 비밀 플레이리스트를 보이지 않는다`() = runTest {
        assertEquals(listOf(PlaylistType.TOTAL), useCase(AUDIO_MEDIA_ID).first().map { it.playlist.type })
    }

    @Test
    fun `비밀모드면 곡이 속한 비밀 플레이리스트도 보인다`() = runTest {
        isSecretModeEnabled.value = true

        assertEquals(listOf(PlaylistType.TOTAL, PlaylistType.SECRET), useCase(AUDIO_MEDIA_ID).first().map { it.playlist.type })
    }

    private fun aggregate(type: PlaylistType): PlaylistTrackAggregate = mockk {
        every { playlist.type } returns type
    }

    private companion object {
        const val AUDIO_MEDIA_ID = 7
    }
}
