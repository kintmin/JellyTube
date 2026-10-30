package com.kintmin.domain

import com.kintmin.domain.audio_edit.model.AudioEditTimeline
import com.kintmin.domain.audio_edit.repository.AudioEditRepository
import com.kintmin.domain.audio_edit.usecase.ApplyAudioEditUseCase
import com.kintmin.domain.audio_edit.usecase.EmptyAudioEditSegment
import com.kintmin.domain.audio_edit.usecase.RevertAudioEditUseCase
import com.kintmin.domain.audio_media.model.AudioMedia
import com.kintmin.log.AppLog
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ApplyAudioEditUseCaseTest {

    private val audioEditRepository: AudioEditRepository = mockk()
    private val appLog: AppLog = mockk(relaxed = true)

    private lateinit var applyAudioEditUseCase: ApplyAudioEditUseCase
    private lateinit var revertAudioEditUseCase: RevertAudioEditUseCase

    private companion object {

        const val AUDIO_MEDIA_ID = 1
        const val ORIGIN_DURATION_MS = 300_000L
    }

    private val editedAudioMedia = AudioMedia(
        id = AUDIO_MEDIA_ID,
        source = "https://example.com/origin",
        name = "원본 음원",
        artist = "아티스트",
        description = "",
        audioDuration = 300.seconds,
        audioFileFullPath = "/audio/origin.m4a",
        imageFileFullPath = null,
        editedAudioFileFullPath = "/audio/edited.m4a",
        editedAudioDuration = 120.seconds,
        createdTime = LocalDateTime(2026, 10, 30, 0, 0),
    )

    @Before
    fun setup() {
        applyAudioEditUseCase = ApplyAudioEditUseCase(
            audioEditRepository = audioEditRepository,
            appLog = appLog,
        )
        revertAudioEditUseCase = RevertAudioEditUseCase(
            audioEditRepository = audioEditRepository,
            appLog = appLog,
        )
    }

    @After
    fun tearDown() {
        clearMocks(audioEditRepository, appLog)
    }

    @Test
    fun `소리 조각이 없으면 리포지토리를 호출하지 않고 실패한다`() = runTest {
        val result = applyAudioEditUseCase(
            audioMediaId = AUDIO_MEDIA_ID,
            timeline = AudioEditTimeline.initial(ORIGIN_DURATION_MS).delete(0),
        )

        assert(result.exceptionOrNull() is EmptyAudioEditSegment)
        coVerify(inverse = true) { audioEditRepository.applyEditedAudio(any(), any()) }
    }

    @Test
    fun `무음 조각만 있으면 리포지토리를 호출하지 않고 실패한다`() = runTest {
        val result = applyAudioEditUseCase(
            audioMediaId = AUDIO_MEDIA_ID,
            // 조각을 모두 지우고 끝 표시만 늘리면 무음만 남는다
            timeline = AudioEditTimeline.initial(ORIGIN_DURATION_MS).delete(0).withEnd(ORIGIN_DURATION_MS + 10_000),
        )

        assert(result.exceptionOrNull() is EmptyAudioEditSegment)
        coVerify(inverse = true) { audioEditRepository.applyEditedAudio(any(), any()) }
    }

    @Test
    fun `소리 조각이 있으면 편집본을 적용하고 갱신된 음원을 반환한다`() = runTest {
        val timeline = AudioEditTimeline.initial(ORIGIN_DURATION_MS).split(60_000).split(120_000).delete(1)
        coEvery {
            audioEditRepository.applyEditedAudio(AUDIO_MEDIA_ID, timeline)
        } returns Result.success(editedAudioMedia)

        val result = applyAudioEditUseCase(
            audioMediaId = AUDIO_MEDIA_ID,
            timeline = timeline,
        )

        assert(result.isSuccess)
        // 편집본이 있으면 재생·표시 길이는 편집본 기준, 원본 길이는 그대로 남는다
        assert(result.getOrThrow().playbackFileFullPath == "/audio/edited.m4a")
        assert(result.getOrThrow().playbackDuration == 120.seconds)
        assert(result.getOrThrow().audioDuration == 300.seconds)
        coVerify(exactly = 1) { audioEditRepository.applyEditedAudio(AUDIO_MEDIA_ID, timeline) }
    }

    @Test
    fun `적용 실패를 그대로 전달한다`() = runTest {
        val timeline = AudioEditTimeline.initial(ORIGIN_DURATION_MS).split(1_000)
        coEvery {
            audioEditRepository.applyEditedAudio(AUDIO_MEDIA_ID, timeline)
        } returns Result.failure(Exception("렌더 실패"))

        val result = applyAudioEditUseCase(
            audioMediaId = AUDIO_MEDIA_ID,
            timeline = timeline,
        )

        assert(result.isFailure)
    }

    @Test
    fun `되돌리면 편집본이 없는 음원을 반환한다`() = runTest {
        val originAudioMedia = editedAudioMedia.copy(
            editedAudioFileFullPath = null,
            editedAudioDuration = null,
        )
        coEvery {
            audioEditRepository.revertEditedAudio(AUDIO_MEDIA_ID)
        } returns Result.success(originAudioMedia)

        val result = revertAudioEditUseCase(audioMediaId = AUDIO_MEDIA_ID)

        assert(result.isSuccess)
        assert(result.getOrThrow().playbackFileFullPath == "/audio/origin.m4a")
        assert(result.getOrThrow().playbackDuration == 300.seconds)
    }
}
