package com.kintmin.domain

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.model.DownloadedMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.AlreadyDownloadedMedia
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.FetchAllPlaylistFlowUseCase
import com.kintmin.log.AppLog
import com.kintmin.log.model.FirebaseEvent
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DownloadAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val deviceStatusRepository: DeviceStatusRepository = mockk()
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase = mockk()
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase = mockk()
    private val fetchAllPlaylistFlowUseCase: FetchAllPlaylistFlowUseCase = mockk()
    private val appLog: AppLog = mockk()

    private lateinit var useCase: DownloadAudioMediaUseCase

    private lateinit var downloadUrl: String
    private lateinit var mockAudioMedia: AudioMedia
    private lateinit var mockDownloadedMedia: DownloadedMedia

    @Before
    fun setup() {
        coEvery { fetchShouldInsertAtTopOnDownloadFlowUseCase() } returns flowOf(false)
        coEvery { fetchPlaylistIdOnDownloadFlowUseCase() } returns flowOf(Playlist.UNCATEGORIZED)
        coEvery { fetchAllPlaylistFlowUseCase() } returns flowOf(
            listOf(
                getMockPlaylist(Playlist.TOTAL, "전체"),
                getMockPlaylist(Playlist.UNCATEGORIZED, "미분류"),
                getMockPlaylist(3, "테스트"),
            )
        )
        coEvery { audioMediaRepository.getAudioMediaBySource(any()) } returns Result.failure(Exception())
        everyLogDefaults()

        useCase = DownloadAudioMediaUseCase(
            audioMediaRepository = audioMediaRepository,
            fetchShouldInsertAtTopOnDownloadFlowUseCase = fetchShouldInsertAtTopOnDownloadFlowUseCase,
            fetchPlaylistIdOnDownloadFlowUseCase = fetchPlaylistIdOnDownloadFlowUseCase,
            fetchAllPlaylistFlowUseCase = fetchAllPlaylistFlowUseCase,
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
            fetchPlaylistIdOnDownloadFlowUseCase,
            fetchAllPlaylistFlowUseCase,
            appLog,
        )
    }

    @Test
    fun `기존 다운로드가 있으면 실패 반환 테스트`() = runTest {
        coEvery { audioMediaRepository.getAudioMediaBySource(downloadUrl) } returns Result.success(mockAudioMedia)

        val result = useCase(downloadUrl)

        assert(result.isFailure)
        assert(result.exceptionOrNull() is AlreadyDownloadedMedia)
        coVerify(inverse = true) { audioMediaRepository.downloadAudioMedia(any()) }
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exception = AlreadyDownloadedMedia(), exactly = 1)
    }

    @Test
    fun `다운로드 성공 시 설정값으로 저장 테스트`() = runTest {
        coEvery { fetchShouldInsertAtTopOnDownloadFlowUseCase() } returns flowOf(true)
        coEvery { fetchPlaylistIdOnDownloadFlowUseCase() } returns flowOf(3)
        coEvery { audioMediaRepository.downloadAudioMedia(downloadUrl) } returns Result.success(mockDownloadedMedia)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = 3,
                shouldInsertAtTopOnDownload = true,
            )
        } returns Result.success(mockAudioMedia to 10)

        val result = useCase(downloadUrl)

        assert(result.isSuccess)
        assert(result.getOrThrow().playlistIdOnDownload == 3)
        assert(result.getOrThrow().shouldInsertAtTopOnDownload)
        verifySuccessDownloadAudioMediaLog(exactly = 1)
        verifyFailedDownloadAudioMediaLog(inverse = true)
    }

    @Test
    fun `선택 재생목록이 없으면 미분류로 fallback 테스트`() = runTest {
        coEvery { fetchPlaylistIdOnDownloadFlowUseCase() } returns flowOf(999)
        coEvery { fetchAllPlaylistFlowUseCase() } returns flowOf(listOf(getMockPlaylist(3, "테스트")))
        coEvery { audioMediaRepository.downloadAudioMedia(downloadUrl) } returns Result.success(mockDownloadedMedia)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = Playlist.UNCATEGORIZED,
                shouldInsertAtTopOnDownload = false,
            )
        } returns Result.success(mockAudioMedia to 10)

        val result = useCase(downloadUrl)

        assert(result.isSuccess)
        assert(result.getOrThrow().playlistIdOnDownload == Playlist.UNCATEGORIZED)
    }

    @Test
    fun `저장 실패 시 실패 로그 전송 테스트`() = runTest {
        val exception = Exception("test")
        coEvery { audioMediaRepository.downloadAudioMedia(downloadUrl) } returns Result.success(mockDownloadedMedia)
        coEvery {
            audioMediaRepository.addAudioMedia(
                downloadedAudioMedia = mockDownloadedMedia,
                playlistIdOnDownload = Playlist.UNCATEGORIZED,
                shouldInsertAtTopOnDownload = false,
            )
        } returns Result.failure(exception)
        coEvery { audioMediaRepository.deleteDownloadedFile(mockDownloadedMedia) } returns Result.success(Unit)

        val result = useCase(downloadUrl)

        assert(result.exceptionOrNull()!!.message == exception.message)
        coVerify(exactly = 1) { audioMediaRepository.deleteDownloadedFile(mockDownloadedMedia) }
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exactly = 1, exception = exception)
    }

    private fun verifySuccessDownloadAudioMediaLog(
        inverse: Boolean = false,
        exactly: Int = -1,
    ) {
        verify(inverse = inverse, exactly = exactly) {
            appLog.sendFirebaseEvent(
                match {
                    it is FirebaseEvent.AddAudioMedia && it.source == downloadUrl
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
            appLog.sendFirebaseEvent(
                match {
                    it is FirebaseEvent.FailedDownloadAudioMedia &&
                            it.source == downloadUrl &&
                            it.exception.message == exception.message
                }
            )
        }
    }

    private fun everyLogDefaults() {
        io.mockk.every { appLog.sendDebugLog(any()) } returns Unit
        io.mockk.every { appLog.sendFirebaseEvent(any()) } returns Unit
        io.mockk.every { appLog.setLogConfig(any()) } returns Unit
        io.mockk.every { deviceStatusRepository.getSystemMemory() } returns Result.failure(Exception())
        io.mockk.every { deviceStatusRepository.getConnectionStatus() } returns Result.failure(Exception())
    }

    private fun getMockPlaylist(id: Int, name: String): Playlist {
        return Playlist(
            id = id,
            name = name,
            description = "",
            audioMediaCount = 0,
            playTimeDuration = 0.seconds,
            createdTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            imageFileFullPath = null,
            isCustomImage = false,
        )
    }
}
