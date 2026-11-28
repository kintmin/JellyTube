package com.kintmin.domain.audio_track

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_track.model.AudioTrack
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListToAddTrackFlowUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.usecase.FetchPlaylistsByTypeUseCase
import com.kintmin.domain.testPlaylist
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchAudioMediaListToAddTrackFlowUseCaseTest {

    private val total = testPlaylist(1, PlaylistType.TOTAL)
    private val secretTotal = testPlaylist(7, PlaylistType.SECRET_TOTAL)
    private val secret = testPlaylist(5, PlaylistType.SECRET)
    private val user = testPlaylist(4, PlaylistType.USER)

    // 곡 10·12 는 일반, 11 은 비밀. 담긴 시각은 10 → 11 → 12 순이다.
    private val repository = mockk<AudioTrackRepository> {
        every { getPlaylistTrackAggregateListFlow(1) } returns flowOf(listOf(aggregate(10, 1, 1), aggregate(12, 1, 3)))
        every { getPlaylistTrackAggregateListFlow(7) } returns flowOf(listOf(aggregate(11, 7, 2)))
        every { getPlaylistTrackAggregateListFlow(5) } returns flowOf(emptyList())
        every { getPlaylistTrackAggregateListFlow(4) } returns flowOf(emptyList())
    }
    private val fetchByType = mockk<FetchPlaylistsByTypeUseCase> {
        every { this@mockk(PlaylistType.TOTAL) } returns flowOf(listOf(total))
        every { this@mockk(PlaylistType.SECRET_TOTAL) } returns flowOf(listOf(secretTotal))
        every { this@mockk(PlaylistType.SECRET) } returns flowOf(listOf(secret))
    }
    private val useCase = FetchAudioMediaListToAddTrackFlowUseCase(repository, fetchByType)

    @Test
    fun `비밀 플레이리스트에 담을 때는 일반 곡과 비밀 곡을 담긴 시각 순으로 섞어 보여 준다`() = runTest {
        assertEquals(listOf(10, 11, 12), useCase(playlistId = 5).first().map { it.audioMedia.id })
    }

    @Test
    fun `일반 플레이리스트에 담을 때는 비밀 곡이 후보에 없다`() = runTest {
        assertEquals(listOf(10, 12), useCase(playlistId = 4).first().map { it.audioMedia.id })
    }

    private fun aggregate(audioMediaId: Int, playlistId: Int, day: Int) = PlaylistTrackAggregate(
        audioMedia = AudioMedia(
            id = audioMediaId,
            source = "https://youtu.be/$audioMediaId",
            name = "곡$audioMediaId",
            artist = "",
            description = "",
            audioDuration = null,
            audioFileFullPath = "$audioMediaId.m4a",
            imageFileFullPath = null,
            createdTime = LocalDateTime(2026, 1, day, 0, 0),
        ),
        playlist = testPlaylist(playlistId, PlaylistType.TOTAL),
        audioTrack = AudioTrack(audioMediaId, playlistId, trackSequence = 1, trackAddedTime = LocalDateTime(2026, 1, day, 0, 0)),
    )
}
