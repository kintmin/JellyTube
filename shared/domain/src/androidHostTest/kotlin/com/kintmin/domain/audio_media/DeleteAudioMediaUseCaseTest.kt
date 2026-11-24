package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.repository.AudioMediaRepository
import com.kintmin.domain.audio_media.usecase.DeleteAudioMediaUseCase
import com.kintmin.domain.audio_track.repository.AudioTrackRepository
import com.kintmin.log.AppLog
import com.kintmin.log.HIDDEN_TEXT
import com.kintmin.log.model.SentryEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAudioMediaUseCaseTest {

    private val audioMediaRepository: AudioMediaRepository = mockk {
        coEvery { deleteAudioMedia(any()) } returns Result.success(Unit)
    }
    private val audioTrackRepository: AudioTrackRepository = mockk {
        every { getSecretAudioMediaIdListFlow() } returns flowOf(listOf(7))
    }
    private val appLog: AppLog = mockk(relaxed = true)
    private val useCase = DeleteAudioMediaUseCase(audioMediaRepository, audioTrackRepository, appLog)

    @Test
    fun `비밀 곡을 지우면 원본 주소를 가린다`() = runTest {
        useCase(7, "https://youtu.be/s")

        verify { appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia(HIDDEN_TEXT, 7)) }
    }

    @Test
    fun `비밀이 아닌 곡을 지우면 원본 주소를 남긴다`() = runTest {
        useCase(8, "https://youtu.be/p")

        verify { appLog.sendSentryEvent(SentryEvent.DeleteAudioMedia("https://youtu.be/p", 8)) }
    }
}
