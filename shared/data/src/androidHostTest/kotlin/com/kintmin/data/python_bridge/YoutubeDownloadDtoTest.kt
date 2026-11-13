package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.YoutubeDownloadDto
import com.kintmin.data.python_bridge.model.withDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeDownloadDtoTest {

    @Test
    fun `비어 온 제목·길이·업로더만 기본값으로 채운다`() {
        val dto = YoutubeDownloadDto(
            title = "",
            thumbnailDownloadUrl = "",
            duration = " ",
            uploader = "",
            description = "",
            audioFileNameWithExt = "a.m4a",
        ).withDefaults()

        assertEquals("알 수 없음", dto.title)
        assertEquals("0", dto.duration)
        assertEquals("알 수 없음", dto.uploader)
        assertEquals("", dto.thumbnailDownloadUrl)
    }

    @Test
    fun `값이 있으면 그대로 둔다`() {
        val dto = YoutubeDownloadDto("곡", "t", "200", "가수", "d", "a.m4a")

        assertEquals(dto, dto.withDefaults())
    }
}
