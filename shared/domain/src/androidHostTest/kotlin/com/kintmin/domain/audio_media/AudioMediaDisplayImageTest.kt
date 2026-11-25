package com.kintmin.domain.audio_media

import com.kintmin.domain.audio_media.model.AudioMedia
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioMediaDisplayImageTest {

    private fun audioMedia(
        imageFileFullPath: String?,
        customImageFileFullPath: String? = null,
    ) = AudioMedia(
        id = 1,
        source = "source",
        name = "name",
        artist = "artist",
        description = "",
        audioDuration = null,
        audioFileFullPath = "/audio.m4a",
        imageFileFullPath = imageFileFullPath,
        customImageFileFullPath = customImageFileFullPath,
        createdTime = LocalDateTime(2026, 1, 1, 0, 0),
    )

    @Test
    fun `고른 썸네일이 있으면 그것을 그린다`() {
        val media = audioMedia(imageFileFullPath = "/origin.webp", customImageFileFullPath = "/custom.webp")

        assertEquals("/custom.webp", media.displayImageFileFullPath)
    }

    @Test
    fun `고른 썸네일이 없으면 정본을 그린다`() {
        assertEquals("/origin.webp", audioMedia(imageFileFullPath = "/origin.webp").displayImageFileFullPath)
    }

    @Test
    fun `받아 온 썸네일이 없는 곡도 고른 썸네일을 그린다`() {
        val media = audioMedia(imageFileFullPath = null, customImageFileFullPath = "/custom.webp")

        assertEquals("/custom.webp", media.displayImageFileFullPath)
    }

    @Test
    fun `둘 다 없으면 그릴 것이 없다`() {
        assertNull(audioMedia(imageFileFullPath = null).displayImageFileFullPath)
    }
}
