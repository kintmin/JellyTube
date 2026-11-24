package com.kintmin.domain

import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_media.model.AddedAudioMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.model.AlreadyDownloadingMedia
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.model.DownloadedYoutubeAudio
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.model.PlaylistType
import com.kintmin.domain.playlist.repository.PlaylistRepository
import com.kintmin.log.AppLog
import com.kintmin.log.model.SentryEvent
import com.kintmin.log.HIDDEN_TEXT
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class DownloadAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val playlistRepository: PlaylistRepository = mockk()
    private val deviceStatusRepository: DeviceStatusRepository = mockk()
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase = mockk()
    private val appLog: AppLog = mockk()

    private lateinit var useCase: DownloadAudioMediaUseCase

    private lateinit var downloadUrl: String
    private lateinit var mockAudioMedia: AudioMedia
    private lateinit var mockDownloadedMedia: DownloadedMedia
    private lateinit var mockYoutubeAudio: DownloadedYoutubeAudio

    @Before
    fun setup() {
        coEvery { fetchShouldInsertAtTopOnDownloadFlowUseCase() } returns flowOf(false)
        coEvery { audioMediaRepository.getAudioMediaBySource(any()) } returns Result.failure(Exception())
        every { audioMediaRepository.reserveDownload(any()) } returns true
        every { audioMediaRepository.releaseDownload(any()) } just Runs
        coEvery { playlistRepository.getPlaylist(any()) } returns Result.success(testPlaylist(UNCATEGORIZED_ID, PlaylistType.USER))
        everyLogDefaults()

        useCase = DownloadAudioMediaUseCase(
            audioMediaRepository = audioMediaRepository,
            playlistRepository = playlistRepository,
            fetchShouldInsertAtTopOnDownloadFlowUseCase = fetchShouldInsertAtTopOnDownloadFlowUseCase,
            appLog = appLog,
            deviceStatusRepository = deviceStatusRepository,
        )

        downloadUrl = "https://example.com/audio.mp3"

        mockAudioMedia = AudioMedia(
            id = 1,
            source = downloadUrl,
            name = "name",
            artist = "artist",
            description = "description",
            audioDuration = 200.seconds,
            audioFileFullPath = "",
            imageFileFullPath = "",
            createdTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        )

        mockYoutubeAudio = DownloadedYoutubeAudio(
            fileName = "a",
            audioFileNameWithExt = "a.mp3",
            title = "name",
            thumbnailUrl = "https://example.com/t.jpg",
            duration = "200",
            uploader = "artist",
            description = "description",
        )
        coEvery { audioMediaRepository.saveThumbnail(any(), any()) } returns Result.failure(Exception())

        mockDownloadedMedia = DownloadedMedia(
            downloadUrl = downloadUrl,
            title = "name",
            audioFileNameWithExt = "a.mp3",
            imageFileNameWithExt = null,
            duration = "200",
            uploader = "artist",
            description = "description",
        )
    }

    @After
    fun tearDown() {
        clearMocks(
            audioMediaRepository,
            deviceStatusRepository,
            fetchShouldInsertAtTopOnDownloadFlowUseCase,
            appLog,
        )
    }

    @Test
    fun `같은 출처를 받는 중이면 받지 않고 남의 예약도 풀지 않는다`() = runTest {
        every { audioMediaRepository.reserveDownload(downloadUrl) } returns false

        val result = useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID)

        assert(result.exceptionOrNull() is AlreadyDownloadingMedia)
        coVerify(inverse = true) { audioMediaRepository.downloadYoutubeAudio(any(), any()) }
        verify(inverse = true) { audioMediaRepository.releaseDownload(any()) }
    }

    @Test
    fun `받기가 끝나면 실패해도 예약을 푼다`() = runTest {
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.failure(IllegalStateException())

        useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID)

        verify(exactly = 1) { audioMediaRepository.releaseDownload(downloadUrl) }
    }

    @Test
    fun `썸네일을 받으면 그 이름을 곡과 함께 넘긴다`() = runTest {
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery { audioMediaRepository.saveThumbnail(mockYoutubeAudio.thumbnailUrl, "a") } returns Result.success("a.webp")
        val withImage = mockDownloadedMedia.copy(imageFileNameWithExt = "a.webp")
        coEvery { audioMediaRepository.addAudioMedia(withImage, UNCATEGORIZED_ID, false) } returns
            Result.success(addedResult(resolvedPlaylistId = UNCATEGORIZED_ID))

        assert(useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID).isSuccess)
    }

    @Test
    fun `음원을 받은 뒤에 취소돼도 행을 만들어 파일만 남지 않게 한다`() = runTest {
        val started = CompletableDeferred<Unit>()
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery { audioMediaRepository.saveThumbnail(any(), any()) } coAnswers {
            started.complete(Unit)
            delay(1_000)
            Result.failure(Exception())
        }
        coEvery { audioMediaRepository.addAudioMedia(mockDownloadedMedia, UNCATEGORIZED_ID, false) } returns
            Result.success(addedResult(resolvedPlaylistId = UNCATEGORIZED_ID))

        val job = launch { useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID) }
        started.await()
        job.cancelAndJoin()

        coVerify(exactly = 1) { audioMediaRepository.addAudioMedia(mockDownloadedMedia, UNCATEGORIZED_ID, false) }
    }

    @Test
    fun `멈춰서 끊긴 받기는 실패로 기록하지 않고 취소를 그대로 던진다`() = runTest {
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } throws CancellationException("stop")

        var caught: Throwable? = null
        try {
            useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID)
        } catch (e: CancellationException) {
            caught = e
        }

        assertEquals("stop", caught?.message)
        verifyFailedDownloadAudioMediaLog(exception = CancellationException("stop"), exactly = 0)
    }

    @Test
    fun `기존 다운로드가 있으면 실패를 돌려주되 받기 실패로 올리지 않는다`() = runTest {
        coEvery { audioMediaRepository.getAudioMediaBySource(downloadUrl) } returns Result.success(mockAudioMedia)

        val result = useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID)

        assert(result.isFailure)
        assert(result.exceptionOrNull() is AlreadyDownloadedMedia)
        coVerify(inverse = true) { audioMediaRepository.downloadYoutubeAudio(any(), any()) }
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exception = AlreadyDownloadedMedia(), exactly = 0)
    }

    @Test
    fun `다운로드 성공 시 전달받은 재생목록으로 저장 테스트`() = runTest {
        coEvery { fetchShouldInsertAtTopOnDownloadFlowUseCase() } returns flowOf(true)
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = 3,
                shouldInsertAtTopOnDownload = true,
            )
        } returns Result.success(addedResult(resolvedPlaylistId = 3))

        val result = useCase(downloadUrl, playlistIdOnDownload = 3)

        assert(result.isSuccess)
        assert(result.getOrThrow().playlistIdOnDownload == 3)
        assert(result.getOrThrow().shouldInsertAtTopOnDownload)
        verifySuccessDownloadAudioMediaLog(exactly = 1)
        verifyFailedDownloadAudioMediaLog(inverse = true)
    }

    @Test
    fun `전달받은 재생목록이 유효하지 않으면 데이터 계층이 해석한 미분류 id를 그대로 반환 테스트`() = runTest {
        // 대상 유효성 검사·미분류 fallback은 데이터 계층(facade) 책임이다.
        // UseCase는 요청값을 그대로 넘기고, 데이터 계층이 해석해 돌려준 id를 surfacing만 한다.
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = 999,
                shouldInsertAtTopOnDownload = false,
            )
        } returns Result.success(addedResult(resolvedPlaylistId = UNCATEGORIZED_ID))

        val result = useCase(downloadUrl, playlistIdOnDownload = 999)

        assert(result.isSuccess)
        assert(result.getOrThrow().playlistIdOnDownload == UNCATEGORIZED_ID)
    }

    @Test
    fun `저장 실패 시 실패 로그 전송 테스트`() = runTest {
        val exception = Exception("test")
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = UNCATEGORIZED_ID,
                shouldInsertAtTopOnDownload = false,
            )
        } returns Result.failure(exception)
        coEvery { audioMediaRepository.deleteDownloadedFile(mockDownloadedMedia) } returns Result.success(Unit)

        val result = useCase(downloadUrl, playlistIdOnDownload = UNCATEGORIZED_ID)

        assert(result.exceptionOrNull()!!.message == exception.message)
        coVerify(exactly = 1) { audioMediaRepository.deleteDownloadedFile(mockDownloadedMedia) }
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exactly = 1, exception = exception)
    }

    @Test
    fun `비밀 플레이리스트로 받으면 주소와 이름을 가린다`() = runTest {
        coEvery { playlistRepository.getPlaylist(SECRET_ID) } returns Result.success(testPlaylist(SECRET_ID, PlaylistType.SECRET))
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.success(mockYoutubeAudio)
        coEvery { audioMediaRepository.addAudioMedia(any(), SECRET_ID, any()) } returns Result.success(addedResult(SECRET_ID))

        useCase(downloadUrl, playlistIdOnDownload = SECRET_ID)

        coVerify { audioMediaRepository.downloadYoutubeAudio(downloadUrl, isSecret = true) }
        verify { appLog.sendDebugLog(match { it.message == "받기 시작: $HIDDEN_TEXT" }) }
        verify {
            appLog.sendSentryEvent(
                match { it is SentryEvent.AddAudioMedia && it.source == HIDDEN_TEXT && it.audioMediaName == HIDDEN_TEXT },
            )
        }
    }

    @Test
    fun `담을 플레이리스트를 확인하지 못하면 비밀로 보고 가린다`() = runTest {
        coEvery { playlistRepository.getPlaylist(SECRET_ID) } returns Result.failure(Exception())
        coEvery { audioMediaRepository.downloadYoutubeAudio(downloadUrl, any()) } returns Result.failure(Exception("boom"))

        useCase(downloadUrl, playlistIdOnDownload = SECRET_ID)

        verify { appLog.sendSentryEvent(match { it is SentryEvent.FailedDownloadAudioMedia && it.source == HIDDEN_TEXT }) }
    }

    private fun verifySuccessDownloadAudioMediaLog(
        inverse: Boolean = false,
        exactly: Int = -1,
    ) {
        verify(inverse = inverse, exactly = exactly) {
            appLog.sendSentryEvent(
                match {
                    it is SentryEvent.AddAudioMedia && it.source == downloadUrl
                }
            )
        }
    }

    private fun verifyFailedDownloadAudioMediaLog(
        exception: Throwable = Exception(),
        inverse: Boolean = false,
        exactly: Int = -1,
    ) {
        verify(inverse = inverse, exactly = exactly) {
            appLog.sendSentryEvent(
                match {
                    it is SentryEvent.FailedDownloadAudioMedia &&
                            it.source == downloadUrl &&
                            it.exception.message == exception.message
                }
            )
        }
    }

    private fun everyLogDefaults() {
        io.mockk.every { appLog.sendDebugLog(any()) } returns Unit
        io.mockk.every { appLog.sendSentryEvent(any()) } returns Unit
        io.mockk.every { appLog.setLogConfig(any()) } returns Unit
        io.mockk.every { deviceStatusRepository.getSystemMemory() } returns Result.failure(Exception())
        io.mockk.every { deviceStatusRepository.getConnectionStatus() } returns Result.failure(Exception())
    }

    private fun addedResult(resolvedPlaylistId: Int, totalPlaylistMediaCount: Int = 10) = AddedAudioMedia(
        audioMedia = mockAudioMedia,
        totalPlaylistMediaCount = totalPlaylistMediaCount,
        totalPlaylistId = TOTAL_ID,
        resolvedPlaylistIdOnDownload = resolvedPlaylistId,
    )

    private companion object {
        // 시스템 플레이리스트는 고정 id가 없지만, 테스트에서는 편의상 관례적 id를 쓴다.
        const val TOTAL_ID = 1
        const val UNCATEGORIZED_ID = 2
        const val SECRET_ID = 5
    }
}
