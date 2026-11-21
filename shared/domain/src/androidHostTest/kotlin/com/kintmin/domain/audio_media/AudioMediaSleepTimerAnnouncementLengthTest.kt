package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
    fun `30초 이하면 마무리 음원으로 고를 수 있다`() {
        assertTrue(audioMedia(30.seconds).fitsSleepTimerAnnouncement)
    }

    @Test
    fun `30초를 넘으면 고를 수 없다`() {
        assertFalse(audioMedia(30.seconds + 1.milliseconds).fitsSleepTimerAnnouncement)
    }

    @Test
    fun `원본이 길어도 편집본이 짧으면 편집본 기준으로 고를 수 있다`() {
        assertTrue(
            audioMedia(audioDuration = 3.seconds * 60, editedAudioDuration = 10.seconds)
                .fitsSleepTimerAnnouncement
        )
    }

    @Test
    fun `원본이 짧아도 편집본이 길면 편집본 기준으로 고를 수 없다`() {
        assertFalse(
            audioMedia(audioDuration = 10.seconds, editedAudioDuration = 45.seconds)
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
