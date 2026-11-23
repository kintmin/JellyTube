package com.kintmin.domain.playlist_download

import com.kintmin.domain.audio_media.model.AgeRestrictedMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedAudioMediaResult
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.audio_track.model.AddedAudioTrack
import com.kintmin.domain.audio_track.model.AudioTrack
import com.kintmin.domain.audio_track.model.PlaylistTrackAggregate
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.lyrics.usecase.DownloadLyricsForAudioMediaUseCase
import com.kintmin.domain.playlist.PlaylistTextLimit
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.domain.playlist.usecase.AddNewPlaylistUseCase
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailure
import com.kintmin.domain.playlist_download.model.PlaylistDownloadFailureReason
import com.kintmin.domain.playlist_download.usecase.DownloadYoutubePlaylistUseCase
import com.kintmin.domain.playlist_download.usecase.PreviewPlaylistDownloadUseCase
import com.kintmin.domain.youtube.model.YoutubePlaylist
import com.kintmin.domain.youtube.model.YoutubePlaylistEntry
import com.kintmin.log.AppLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDownloadUseCaseTest {

    private fun audio(id: Int, source: String) = mockk<AudioMedia> {
        every { this@mockk.id } returns id
        every { this@mockk.source } returns source
        every { name } returns "곡$id"
    }

    private fun userPlaylist(id: Int, name: String) = mockk<Playlist> {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { type } returns PlaylistType.USER
    }

    private fun entry(id: String, available: Boolean = true, duration: Long? = 100) =
        YoutubePlaylistEntry(videoId = id, title = "제목 $id", durationSeconds = duration, isAvailable = available)

    private var owned = listOf<AudioMedia>()
    private var playlists = listOf<Playlist>()
    /** 대상 재생목록의 곡 순서. 담기와 옮기기를 실제처럼 반영한다. */
    private val tracks = mutableListOf<Int>()

    private val audioMediaRepository = mockk<AudioMediaRepository> {
        every { getAudioMediaListFlow() } answers { flowOf(owned) }
    }
    private val playlistRepository = mockk<PlaylistRepository> {
        every { getAllPlaylistFlow() } answers { flowOf(playlists) }
    }
    private val addNewPlaylist = mockk<AddNewPlaylistUseCase> {
        coEvery { this@mockk(any()) } returns Result.success(50)
    }
    private val audioTrackRepository = mockk<AudioTrackRepository> {
        every { getPlaylistTrackAggregateListFlow(any()) } answers {
            val playlistId = firstArg<Int>()
            flowOf(
                tracks.mapIndexed { index, id ->
                    mockk<PlaylistTrackAggregate> {
                        every { audioMedia } returns audio(id, "")
                        every { audioTrack } returns mockk<AudioTrack> {
                            every { audioMediaId } returns id
                            every { this@mockk.playlistId } returns playlistId
                            every { trackSequence } returns index + 1
                        }
                    }
                },
            )
        }
        coEvery { addCustomAudioTrack(any(), any()) } answers {
            tracks += secondArg<List<Int>>()
            Result.success(AddedAudioTrack(unlinkedAudioMediaCount = 0))
        }
        coEvery { reorderTracks(any(), any()) } answers {
            tracks.clear()
            tracks += secondArg<List<Int>>()
            Result.success(Unit)
        }
    }
    private var nextId = 100
    private val downloadAudio = mockk<DownloadAudioMediaUseCase> {
        coEvery { this@mockk(any(), true) } answers {
            val result = mockk<DownloadedAudioMediaResult> { every { audioMedia } returns audio(nextId++, firstArg()) }
            Result.success(result)
        }
    }
    private val downloadLyrics = mockk<DownloadLyricsForAudioMediaUseCase> {
        coEvery { this@mockk(any(), any(), any()) } returns Result.success(Unit)
    }

    private fun downloadUseCase() = DownloadYoutubePlaylistUseCase(
        audioMediaRepository, audioTrackRepository, playlistRepository, addNewPlaylist, downloadAudio, downloadLyrics,
        mockk<AppLog>(relaxed = true),
    )

    @Test
    fun `다른 주소로 이미 받은 곡은 다시 받지 않고 받은 곡과 함께 유튜브 순서대로 새 재생목록에 담는다`() = runTest {
        owned = listOf(audio(7, "https://youtu.be/b?si=x"))
        val playlist = YoutubePlaylist(" 드라이브 ", listOf(entry("a"), entry("b"), entry("c")))

        val summary = downloadUseCase()(playlist) {}.getOrThrow()

        coVerify(exactly = 0) { downloadAudio("https://www.youtube.com/watch?v=b", any()) }
        coVerify { addNewPlaylist("드라이브") }
        assertEquals(listOf(100, 7, 101), tracks)
        assertEquals(50, summary.playlistId)
        assertEquals(2, summary.downloadedCount)
        assertEquals(1, summary.alreadyOnDeviceCount)
    }

    @Test
    fun `같은 이름의 재생목록이 있으면 거기에 합치고 이미 담긴 곡은 다시 담지 않는다`() = runTest {
        owned = listOf(audio(7, "https://www.youtube.com/watch?v=a"))
        playlists = listOf(userPlaylist(9, "드라이브"))
        tracks += 7

        val summary = downloadUseCase()(YoutubePlaylist("드라이브", listOf(entry("a"), entry("b")))) {}.getOrThrow()

        coVerify(exactly = 0) { addNewPlaylist(any()) }
        assertEquals(listOf(7, 100), tracks)
        assertEquals(9, summary.playlistId)
    }

    @Test
    fun `비공개 곡과 받지 못한 곡은 이유와 함께 남기고 나머지는 담는다`() = runTest {
        coEvery { downloadAudio("https://www.youtube.com/watch?v=b", true) } returns Result.failure(IllegalStateException("429"))

        val summary = downloadUseCase()(
            YoutubePlaylist("목록", listOf(entry("a"), entry("b"), entry("x", available = false))),
        ) {}.getOrThrow()

        assertEquals(
            listOf(
                PlaylistDownloadFailure("제목 x", PlaylistDownloadFailureReason.UNAVAILABLE),
                PlaylistDownloadFailure("제목 b", PlaylistDownloadFailureReason.DOWNLOAD_FAILED),
            ),
            summary.failures,
        )
        assertEquals(listOf(100), tracks)
        coVerify(exactly = 2) { downloadAudio("https://www.youtube.com/watch?v=b", true) }
    }

    @Test
    fun `멈춰도 그때까지 받은 곡은 재생목록에 담는다`() = runTest {
        coEvery { downloadAudio("https://www.youtube.com/watch?v=b", true) } coAnswers { awaitCancellation() }
        val job = launch { downloadUseCase()(YoutubePlaylist("목록", listOf(entry("a"), entry("b")))) {} }
        advanceUntilIdle()

        job.cancelAndJoin()

        coVerify { addNewPlaylist("목록") }
        assertEquals(listOf(100), tracks)
    }

    @Test
    fun `다시 받으면 빠졌던 곡을 유튜브 순서의 제자리에 끼운다`() = runTest {
        owned = listOf(audio(1, "https://www.youtube.com/watch?v=a"), audio(3, "https://www.youtube.com/watch?v=c"))
        playlists = listOf(userPlaylist(9, "목록"))
        tracks += listOf(1, 3)

        downloadUseCase()(YoutubePlaylist("목록", listOf(entry("a"), entry("b"), entry("c")))) {}.getOrThrow()

        assertEquals(listOf(1, 100, 3), tracks)
    }

    @Test
    fun `연령 제한 영상은 다시 시도하지 않고 이유를 따로 남긴다`() = runTest {
        coEvery { downloadAudio("https://www.youtube.com/watch?v=b", true) } returns Result.failure(AgeRestrictedMedia())

        val summary = downloadUseCase()(YoutubePlaylist("목록", listOf(entry("a"), entry("b")))) {}.getOrThrow()

        assertEquals(listOf(PlaylistDownloadFailure("제목 b", PlaylistDownloadFailureReason.AGE_RESTRICTED)), summary.failures)
        coVerify(exactly = 1) { downloadAudio("https://www.youtube.com/watch?v=b", true) }
    }

    @Test
    fun `받아 둔 곡은 유튜브에서 비공개가 돼도 재생목록에 담는다`() = runTest {
        owned = listOf(audio(7, "https://www.youtube.com/watch?v=a"))

        val summary = downloadUseCase()(YoutubePlaylist("목록", listOf(entry("a", available = false), entry("b")))) {}.getOrThrow()

        assertEquals(listOf(7, 100), tracks)
        assertEquals(1, summary.alreadyOnDeviceCount)
        assertTrue(summary.failures.isEmpty())
    }

    @Test
    fun `가사를 찾는 사이에 멈춰도 받은 곡은 재생목록에 담는다`() = runTest {
        coEvery { downloadLyrics(any(), any(), any()) } coAnswers { awaitCancellation() }
        val job = launch { downloadUseCase()(YoutubePlaylist("목록", listOf(entry("a")))) {} }
        advanceUntilIdle()

        job.cancelAndJoin()

        assertEquals(listOf(100), tracks)
    }

    @Test
    fun `담을 곡이 하나도 없으면 빈 재생목록을 만들지 않는다`() = runTest {
        val summary = downloadUseCase()(YoutubePlaylist("목록", listOf(entry("x", available = false)))) {}.getOrThrow()

        assertNull(summary.playlistId)
        coVerify(exactly = 0) { addNewPlaylist(any()) }
    }

    @Test
    fun `상한을 넘는 유튜브 제목은 잘린 이름으로 재생목록을 만든다`() = runTest {
        downloadUseCase()(YoutubePlaylist("가".repeat(150), listOf(entry("a")))) {}.getOrThrow()

        coVerify { addNewPlaylist("가".repeat(PlaylistTextLimit.MAX_TITLE_LENGTH)) }
    }

    @Test
    fun `상한까지 같은 이름이면 유튜브 원제가 달라도 그 재생목록에 합친다`() = runTest {
        playlists = listOf(userPlaylist(9, "가".repeat(PlaylistTextLimit.MAX_TITLE_LENGTH)))

        val summary = downloadUseCase()(YoutubePlaylist("가".repeat(150), listOf(entry("a")))) {}.getOrThrow()

        assertEquals(9, summary.playlistId)
        coVerify(exactly = 0) { addNewPlaylist(any()) }
    }

    @Test
    fun `미리보기는 같은 영상을 한 번만 세고 받을 곡만 용량에 넣는다`() = runTest {
        owned = listOf(audio(7, "https://youtu.be/a"))
        playlists = listOf(userPlaylist(9, "드라이브 "))
        coEvery { audioMediaRepository.fetchYoutubePlaylist("https://www.youtube.com/playlist?list=PLx") } returns Result.success(
            YoutubePlaylist("드라이브", listOf(entry("a"), entry("b", duration = 200), entry("b"), entry("x", available = false), entry("c", duration = null))),
        )

        val preview = PreviewPlaylistDownloadUseCase(audioMediaRepository, playlistRepository)("https://m.youtube.com/watch?v=a&list=PLx&index=2").getOrThrow()

        assertEquals(4, preview.totalCount)
        assertEquals(1, preview.alreadyOnDeviceCount)
        assertEquals(2, preview.downloadCount)
        assertEquals(1, preview.unavailableCount)
        assertEquals((200 + 240) * 16_000L, preview.estimatedDownloadBytes)
        assertTrue(preview.mergesIntoExistingPlaylist)
    }

    @Test
    fun `미리보기는 같은 이름의 재생목록이 없으면 새로 만든다고 알린다`() = runTest {
        coEvery { audioMediaRepository.fetchYoutubePlaylist("https://www.youtube.com/playlist?list=PLx") } returns Result.success(YoutubePlaylist("새 목록", listOf(entry("a"))))

        assertFalse(PreviewPlaylistDownloadUseCase(audioMediaRepository, playlistRepository)("https://m.youtube.com/watch?v=a&list=PLx&index=2").getOrThrow().mergesIntoExistingPlaylist)
    }

    @Test
    fun `미리보기는 받아 둔 곡이 비공개가 돼도 이미 있는 곡으로 센다`() = runTest {
        owned = listOf(audio(7, "https://youtu.be/a"))
        coEvery { audioMediaRepository.fetchYoutubePlaylist("https://www.youtube.com/playlist?list=PLx") } returns
            Result.success(YoutubePlaylist("목록", listOf(entry("a", available = false), entry("x", available = false))))

        val preview = PreviewPlaylistDownloadUseCase(audioMediaRepository, playlistRepository)("https://m.youtube.com/playlist?list=PLx").getOrThrow()

        assertEquals(1, preview.alreadyOnDeviceCount)
        assertEquals(1, preview.unavailableCount)
    }
}
