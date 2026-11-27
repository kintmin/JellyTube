package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AudioMediaSleepTimerAnnouncementLengthTest {

    private fun audioMedia(
        audioDuration: Duration?,
        editedAudioDuration: Duration? = null,
    ) = AudioMedia(
        id = 1,
        source = "source",
        name = "name",
        artist = "artist",
        description = "",
        audioDuration = audioDuration,
        audioFileFullPath = "/audio.m4a",
        imageFileFullPath = null,
        editedAudioFileFullPath = editedAudioDuration?.let { "/edited.m4a" },
        editedAudioDuration = editedAudioDuration,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )

    @Test
    fun `길이를 알면 마무리 음원으로 고를 수 있다`() {
        assertTrue(audioMedia(10.seconds).fitsSleepTimerAnnouncement)
    }

    @Test
    fun `길이에 상한이 없어 긴 음원도 고를 수 있다`() {
        assertTrue(audioMedia(5.seconds * 60).fitsSleepTimerAnnouncement)
    }

    @Test
    fun `원본 길이를 몰라도 편집본 길이를 알면 고를 수 있다`() {
        assertTrue(
            audioMedia(audioDuration = null, editedAudioDuration = 10.seconds)
                .fitsSleepTimerAnnouncement
        )
    }

    @Test
    fun `길이를 모르면 끝날 때를 알 수 없으므로 고를 수 없다`() {
        assertFalse(audioMedia(audioDuration = null).fitsSleepTimerAnnouncement)
    }

    @Test
    fun `벨소리 길이 판정과 길이를 모를 때의 답이 반대다`() {
        val unknownLength = audioMedia(audioDuration = null)
        assertTrue(unknownLength.fitsRingtoneLength)
        assertFalse(unknownLength.fitsSleepTimerAnnouncement)
    }
}
