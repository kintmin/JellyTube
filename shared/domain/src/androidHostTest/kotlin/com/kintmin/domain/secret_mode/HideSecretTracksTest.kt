package com.kintmin.domain.secret_mode

import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.model.SecretTrack
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.audio_track.usecase.FetchAudioMediaListFlowUseCase
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.testPlaylist
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class HideSecretTracksTest {

    private val secretTracks = listOf(SecretTrack(2, 30.seconds), SecretTrack(3, 40.seconds))

    @Test
    fun `전체의 곡 수와 길이에서 비밀 곡을 뺀다`() {
        val total = testPlaylist(1, PlaylistType.TOTAL, audioMediaCount = 5, playTimeDuration = 300.seconds).hidingSecretTracks(secretTracks)

        assertEquals(3, total.audioMediaCount)
        assertEquals(230.seconds, total.playTimeDuration)
    }

    @Test
    fun `전체가 아닌 플레이리스트는 그대로다`() {
        listOf(PlaylistType.USER, PlaylistType.FAVORITE, PlaylistType.UNCATEGORIZED, PlaylistType.SECRET).forEach { type ->
            val playlist = testPlaylist(1, type, audioMediaCount = 5, playTimeDuration = 300.seconds)
            assertEquals(playlist, playlist.hidingSecretTracks(secretTracks))
        }
    }

    @Test
    fun `전체 곡 목록에서만 비밀 곡을 거른다`() = runTest {
        val repository: AudioTrackRepository = mockk {
            every { getPlaylistTrackAggregateListFlow(1) } returns flowOf(listOf(track(1, PlaylistType.TOTAL), track(2, PlaylistType.TOTAL)))
            every { getPlaylistTrackAggregateListFlow(4) } returns flowOf(listOf(track(1, PlaylistType.USER), track(2, PlaylistType.USER)))
            every { getSecretTrackListFlow() } returns flowOf(secretTracks)
        }
        val playlistRepository: PlaylistRepository = mockk {
            every { getPlaylistFlow(1) } returns flowOf(testPlaylist(1, PlaylistType.TOTAL))
            every { getPlaylistFlow(4) } returns flowOf(testPlaylist(4, PlaylistType.USER))
        }
        val useCase = FetchAudioMediaListFlowUseCase(repository, playlistRepository)

        assertEquals(listOf(1), useCase(1).first().map { it.audioMedia.id })
        assertEquals(listOf(1, 2), useCase(4).first().map { it.audioMedia.id })
    }

    private fun track(audioMediaId: Int, type: PlaylistType): PlaylistTrackAggregate = mockk {
        every { audioMedia.id } returns audioMediaId
        every { playlist.type } returns type
    }

}
