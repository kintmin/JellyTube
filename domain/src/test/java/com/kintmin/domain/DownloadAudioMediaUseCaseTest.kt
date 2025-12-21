package com.kintmin.domain

import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.DownloadAudioMediaUseCase
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.domain.device.repository.DeviceStatusRepository
import com.kintmin.domain.playlist.model.Playlist
import com.kintmin.domain.playlist.usecase.internal.UpdateOnPlaylistChangeUseCase
import com.kintmin.log.FirebaseEvent
import com.kintmin.log.Log
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class DownloadAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val audioTrackRepository: AudioTrackRepository = mockk()
    private val deviceStatusRepository: DeviceStatusRepository = mockk()
    private val updateOnPlaylistChangeUseCase: UpdateOnPlaylistChangeUseCase = mockk()
    private val log: Log = mockk()

    private lateinit var useCase: DownloadAudioMediaUseCase

    private lateinit var downloadUrl: String
    private lateinit var mockAudioMedia: AudioMedia

    @Before
    fun setup() {
        coEvery {
            deviceStatusRepository.getSystemMemory()
        } returns Result.failure(Exception())
        coEvery {
            deviceStatusRepository.getConnectionStatus()
        } returns Result.failure(Exception())
        coEvery {
            log.sendFirebaseEvent(any())
        } returns Unit

        useCase = DownloadAudioMediaUseCase(
            audioMediaRepository = audioMediaRepository,
            audioTrackRepository = audioTrackRepository,
            updateOnPlaylistChangeUseCase = updateOnPlaylistChangeUseCase,
            log = log,
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
            createdTime = LocalDateTime.now(),
        )
    }

    @After
    fun tearDown() {
        clearMocks(
            audioMediaRepository,
            audioTrackRepository,
            deviceStatusRepository,
            updateOnPlaylistChangeUseCase,
            log,
        )
    }

    @Test
    fun `다운한 목록 중 동일한 출처가 있을 경우 즉시 반환 테스트`() = runTest {
        coEvery {
            audioMediaRepository.getAudioMediaBySource(downloadUrl)
        } returns Result.success(mockk())

        val result = useCase(downloadUrl)

        assert(result.isSuccess)
        coVerify(inverse = true) { audioMediaRepository.addAudioMedia(downloadUrl) }
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(inverse = true)
    }

    @Test
    fun `미디어 추가 실패 시 오류 전파 테스트`() = runTest {
        val exception = Exception("test")
        coEvery {
            audioMediaRepository.getAudioMediaBySource(downloadUrl)
        } returns Result.failure(Exception())
        coEvery {
            audioMediaRepository.addAudioMedia(downloadUrl)
        } returns Result.failure(exception)

        val result = useCase(downloadUrl)

        assert(result.exceptionOrNull()!!.message == exception.message)
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exactly = 1, exception = exception)
    }

    @Test
    fun `재생목록 전체에 추가 실패 시 오류 전파 테스트`() = runTest {
        val exception = Exception("test")
        coEvery {
            audioMediaRepository.getAudioMediaBySource(downloadUrl)
        } returns Result.failure(Exception())
        coEvery {
            audioMediaRepository.addAudioMedia(downloadUrl)
        } returns Result.success(mockAudioMedia)
        coEvery {
            audioTrackRepository.addAudioTrack(Playlist.TOTAL, any())
        } returns Result.failure(exception)

        val result = useCase(downloadUrl)

        assert(result.exceptionOrNull()!!.message == exception.message)
        verifySuccessDownloadAudioMediaLog(inverse = true)
        verifyFailedDownloadAudioMediaLog(exactly = 1, exception = exception)
    }

    @Test
    fun `미분류 추가 실패 시 오류 무시 테스트`() = runTest {
        val exception = Exception("test")
        coEvery {
            audioMediaRepository.getAudioMediaBySource(downloadUrl)
        } returns Result.failure(Exception())
        coEvery {
            audioMediaRepository.addAudioMedia(downloadUrl)
        } returns Result.success(mockAudioMedia)
        coEvery {
            audioTrackRepository.addAudioTrack(Playlist.TOTAL, any())
        } returns Result.success(1)
        coEvery {
            audioTrackRepository.addAudioTrack(Playlist.UNCATEGORIZED, any())
        } returns Result.failure(exception)
        coEvery {
            updateOnPlaylistChangeUseCase(any())
        } returns Result.success(Unit)

        val result = useCase(downloadUrl)

        assert(result.isSuccess)
        coVerify(exactly = 1) { updateOnPlaylistChangeUseCase(Playlist.TOTAL) }
        coVerify(exactly = 1) { updateOnPlaylistChangeUseCase(Playlist.UNCATEGORIZED) }
        verifySuccessDownloadAudioMediaLog(exactly = 1)
        verifyFailedDownloadAudioMediaLog(inverse = true)
    }

    @Test
    fun `플레이리스트 업데이트 실패 시 오류 무시 테스트`() = runTest {
        val exception = Exception("test")
        coEvery {
            audioMediaRepository.getAudioMediaBySource(downloadUrl)
        } returns Result.failure(Exception())
        coEvery {
            audioMediaRepository.addAudioMedia(downloadUrl)
        } returns Result.success(mockAudioMedia)
        coEvery {
            audioTrackRepository.addAudioTrack(any(), any())
        } returns Result.success(1)
        coEvery {
            updateOnPlaylistChangeUseCase(any())
        } returns Result.failure(exception)

        val result = useCase(downloadUrl)

        assert(result.isSuccess)
        verifySuccessDownloadAudioMediaLog(exactly = 1)
        verifyFailedDownloadAudioMediaLog(inverse = true)
    }

    private fun verifySuccessDownloadAudioMediaLog(
        inverse: Boolean = false,
        exactly: Int = -1,
    ) {
        coVerify(inverse = inverse, exactly = exactly) {
            log.sendFirebaseEvent(
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
        coVerify(inverse = inverse, exactly = exactly) {
            log.sendFirebaseEvent(
                match {
                    it is FirebaseEvent.FailedDownloadAudioMedia &&
                            it.source == downloadUrl &&
                            it.exception.message == exception.message
                }
            )
        }
    }
}