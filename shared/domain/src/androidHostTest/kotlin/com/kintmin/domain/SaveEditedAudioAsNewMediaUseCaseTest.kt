package com.kintmin.domain

import com.kintmin.domain.app_setting.usecase.FetchPlaylistIdOnDownloadFlowUseCase
import com.kintmin.domain.app_setting.usecase.FetchShouldInsertAtTopOnDownloadFlowUseCase
import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_edit.model.BlankEditedAudioName
import com.kintmin.domain.audio_edit.usecase.SaveEditedAudioAsNewMediaUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.lyrics.model.EditedAudioLyrics
import com.kintmin.domain.lyrics.usecase.ApplyLyricsToAudioMediaUseCase
import com.kintmin.domain.lyrics.usecase.BuildEditedAudioLyricsUseCase
import com.kintmin.log.AppLog
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SaveEditedAudioAsNewMediaUseCaseTest {

    private companion object {

        const val AUDIO_MEDIA_ID = 1
        const val NEW_AUDIO_MEDIA_ID = 2
        const val ORIGIN_LYRIC_PATH = "/lyric/origin.lrc"
    }

    private val audioEditRepository: AudioEditRepository = mockk()
    private val audioMediaRepository: AudioMediaRepository = mockk()
    private val buildEditedAudioLyrics: BuildEditedAudioLyricsUseCase = mockk()
    private val applyLyricsToAudioMedia: ApplyLyricsToAudioMediaUseCase = mockk()
    private val fetchShouldInsertAtTopOnDownloadFlowUseCase: FetchShouldInsertAtTopOnDownloadFlowUseCase = mockk()
    private val fetchPlaylistIdOnDownloadFlowUseCase: FetchPlaylistIdOnDownloadFlowUseCase = mockk()
    private val appLog: AppLog = mockk(relaxed = true)

    private val useCase = SaveEditedAudioAsNewMediaUseCase(
        audioEditRepository = audioEditRepository,
        audioMediaRepository = audioMediaRepository,
        buildEditedAudioLyricsUseCase = buildEditedAudioLyrics,
        applyLyricsToAudioMediaUseCase = applyLyricsToAudioMedia,
        fetchShouldInsertAtTopOnDownloadFlowUseCase = fetchShouldInsertAtTopOnDownloadFlowUseCase,
        fetchPlaylistIdOnDownloadFlowUseCase = fetchPlaylistIdOnDownloadFlowUseCase,
        appLog = appLog,
    )

    private val parts = listOf(AudioEditPart.Audio(startMs = 0, endMs = 1_000))

    private val originAudioMedia = AudioMedia(
        id = AUDIO_MEDIA_ID,
        source = "https://example.com/origin",
        name = "원본 음원",
        artist = "아티스트",
        description = "",
        audioDuration = 300.seconds,
        audioFileFullPath = "/audio/origin.m4a",
        imageFileFullPath = null,
        lyricFileFullPath = ORIGIN_LYRIC_PATH,
        createdTime = LocalDateTime(2026, 11, 27, 0, 0),
    )

    @Before
    fun setup() {
        every { audioEditRepository.reserveSaving(any()) } returns true
        every { audioEditRepository.releaseSaving(any()) } just Runs
    }

    @Test
    fun `이름이 비어 실패해도 저장 중 표시를 푼다`() = runTest {
        val result = useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = " ")

        assertTrue(result.exceptionOrNull() is BlankEditedAudioName)
        verify(exactly = 1) { audioEditRepository.releaseSaving(AUDIO_MEDIA_ID) }
        coVerify(inverse = true) { audioEditRepository.saveEditedAudioAsNewMedia(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `설정을 읽다 예외가 나도 저장 중 표시를 푼다`() = runTest {
        every { fetchPlaylistIdOnDownloadFlowUseCase() } throws IllegalStateException("설정 읽기 실패")

        try {
            useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = "편집본")
            fail("예외가 전달되어야 한다")
        } catch (e: IllegalStateException) {
            verify(exactly = 1) { audioEditRepository.releaseSaving(AUDIO_MEDIA_ID) }
        }
    }

    @Test
    fun `새 곡으로 저장하면 조각 배치에 맞춘 가사를 물려준다`() = runTest {
        givenSaveSucceeds()
        coEvery { audioMediaRepository.getAudioMedia(AUDIO_MEDIA_ID) } returns Result.success(originAudioMedia)
        coEvery { buildEditedAudioLyrics(ORIGIN_LYRIC_PATH, parts) } returns EditedAudioLyrics(
            synced = "[00:00.00]가사",
            translation = "[00:00.00]뜻",
            transliteration = null,
        )
        coEvery { applyLyricsToAudioMedia(any(), any(), any(), any(), any(), any()) } returns Result.success("/lyric/new.lrc")

        val result = useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = "편집본")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            applyLyricsToAudioMedia(
                audioMediaId = NEW_AUDIO_MEDIA_ID,
                plainLyrics = null,
                syncedLyrics = "[00:00.00]가사",
                translationLyrics = "[00:00.00]뜻",
                transliterationLyrics = null,
                previousLyricFileFullPath = null,
            )
        }
    }

    @Test
    fun `원본에 가사가 없으면 새 곡에도 붙이지 않는다`() = runTest {
        givenSaveSucceeds()
        coEvery { audioMediaRepository.getAudioMedia(AUDIO_MEDIA_ID) } returns
            Result.success(originAudioMedia.copy(lyricFileFullPath = null))

        val result = useCase(audioMediaId = AUDIO_MEDIA_ID, parts = parts, name = "편집본")

        assertTrue(result.isSuccess)
        coVerify(inverse = true) { applyLyricsToAudioMedia(any(), any(), any(), any(), any(), any()) }
    }

    private fun givenSaveSucceeds() {
        every { fetchPlaylistIdOnDownloadFlowUseCase() } returns flowOf(null)
        every { fetchShouldInsertAtTopOnDownloadFlowUseCase() } returns flowOf(false)
        coEvery {
            audioEditRepository.saveEditedAudioAsNewMedia(any(), any(), any(), any(), any())
        } returns Result.success(originAudioMedia.copy(id = NEW_AUDIO_MEDIA_ID, lyricFileFullPath = null))
    }
}
