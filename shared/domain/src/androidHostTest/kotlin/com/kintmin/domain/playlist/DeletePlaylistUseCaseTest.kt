package com.kintmin.domain.playlist

import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.DeletePlaylistUseCase
import com.kintmin.domain.testPlaylist
import com.kintmin.log.AppLog
import com.kintmin.log.HIDDEN_TEXT
import com.kintmin.log.model.SentryEvent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeletePlaylistUseCaseTest {

    private val playlistRepository: PlaylistRepository = mockk {
        coEvery { deletePlaylist(any()) } returns Result.success(Unit)
    }
    private val audioTrackRepository: AudioTrackRepository = mockk {
        coEvery { getPlaylistTrackCount(any()) } returns Result.success(2)
    }
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = DeletePlaylistUseCase(audioTrackRepository, playlistRepository, appLog)

    @Test
    fun `비밀 플레이리스트를 지우면 제목을 가린다`() = runTest {
        coEvery { playlistRepository.getPlaylist(5) } returns Result.success(testPlaylist(5, PlaylistType.SECRET))

        useCase(5)

        verify { appLog.sendSentryEvent(SentryEvent.DeletePlaylist(5, HIDDEN_TEXT, 2)) }
    }

    @Test
    fun `일반 플레이리스트를 지우면 제목을 남긴다`() = runTest {
        coEvery { playlistRepository.getPlaylist(6) } returns Result.success(testPlaylist(6, PlaylistType.USER))

        useCase(6)

        verify { appLog.sendSentryEvent(SentryEvent.DeletePlaylist(6, "p6", 2)) }
    }
}
