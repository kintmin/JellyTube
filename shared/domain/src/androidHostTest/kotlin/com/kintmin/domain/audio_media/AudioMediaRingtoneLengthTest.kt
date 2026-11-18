package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AudioMediaRingtoneLengthTest {

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
    fun `30초 미만이면 벨소리 길이에 들어간다`() {
        assertTrue(audioMedia(29.seconds + 999.milliseconds).fitsRingtoneLength)
    }

    @Test
    fun `정확히 30초는 시스템이 거부하므로 들어가지 않는다`() {
        assertFalse(audioMedia(30.seconds).fitsRingtoneLength)
    }

    @Test
    fun `원본이 길어도 편집본이 짧으면 편집본 기준으로 들어간다`() {
        assertTrue(audioMedia(audioDuration = 3.seconds * 60, editedAudioDuration = 10.seconds).fitsRingtoneLength)
    }

    @Test
    fun `원본이 짧아도 편집본이 길면 편집본 기준으로 들어가지 않는다`() {
        assertFalse(audioMedia(audioDuration = 10.seconds, editedAudioDuration = 45.seconds).fitsRingtoneLength)
    }

    @Test
    fun `길이를 모르면 막지 않는다`() {
        assertTrue(audioMedia(audioDuration = null).fitsRingtoneLength)
    }
}
