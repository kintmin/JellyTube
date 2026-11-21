package com.kintmin.data.local_db

import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.local_db.model.clampText
import com.kintmin.domain.audio_media.AudioMediaTextLimit
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioMediaEntityTextLimitTest {

    @Test
    fun `제목·아티스트·설명을 도메인 상한으로 자르고 나머지는 건드리지 않는다`() {
        val entity = AudioMediaEntity(
            source = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            name = "a".repeat(300),
            artist = "b".repeat(300),
            description = "c".repeat(9000),
            audioFileNameWithExt = "a.m4a",
        )

        val clamped = entity.clampText()

        assertEquals(AudioMediaTextLimit.MAX_NAME_LENGTH, clamped.name.length)
        assertEquals(AudioMediaTextLimit.MAX_ARTIST_LENGTH, clamped.artist.length)
        assertEquals(AudioMediaTextLimit.MAX_DESCRIPTION_LENGTH, clamped.description.length)
        assertEquals(entity.copy(name = clamped.name, artist = clamped.artist, description = clamped.description), clamped)
    }
}
