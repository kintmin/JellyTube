package com.kintmin.data.python_bridge

import com.kintmin.data.python_bridge.model.parseYoutubePlaylist
import com.kintmin.domain.youtube.model.YoutubePlaylist
import com.kintmin.domain.youtube.model.YoutubePlaylistEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubePlaylistDtoTest {

    @Test
    fun `소수 길이와 빠진 길이, 받을 수 없는 영상을 읽는다`() {
        val json = """
            {"title": "드라이브", "entries": [
              {"id": "a", "title": "곡 A", "duration": 215.0, "available": true},
              {"id": "b", "title": "[Private video]", "duration": null, "available": false, "extra": 1}
            ]}
        """.trimIndent()

        assertEquals(
            YoutubePlaylist(
                "드라이브",
                listOf(
                    YoutubePlaylistEntry("a", "곡 A", 215, true),
                    YoutubePlaylistEntry("b", "[Private video]", null, false),
                ),
            ),
            parseYoutubePlaylist(json),
        )
    }
}
