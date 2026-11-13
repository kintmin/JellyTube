package com.kintmin.data.python_bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HlsMediaPlaylistTest {

    private val base = "https://rr1.googlevideo.com/api/manifest/hls_playlist/itag/234/index.m3u8?x=1"

    /** 유튜브 234 재생목록의 실제 태그 구성이다. */
    private val youtube = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-PLAYLIST-TYPE:VOD
        #EXT-X-TARGETDURATION:7
        #EXTINF:5.0,
        https://rr1.googlevideo.com/videoplayback/id/1/sq/0/file/seg.ts
        #EXTINF:4.5,
        https://rr1.googlevideo.com/videoplayback/id/1/sq/1/file/seg.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    @Test
    fun `유튜브 VOD 재생목록은 조각 주소를 순서대로 돌려준다`() {
        val playlist = HlsMediaPlaylist.parse(youtube, base)

        assertNull(playlist.unsupportedReason)
        assertEquals(
            listOf(
                "https://rr1.googlevideo.com/videoplayback/id/1/sq/0/file/seg.ts",
                "https://rr1.googlevideo.com/videoplayback/id/1/sq/1/file/seg.ts",
            ),
            playlist.segmentUrls,
        )
    }

    @Test
    fun `허용 목록 밖의 태그는 이름을 모아 사유로 돌려준다`() {
        val withKey = youtube.replace("#EXTINF:5.0,", "#EXT-X-KEY:METHOD=AES-128,URI=\"k\"\n#EXTINF:5.0,")
        val withMap = youtube.replace(
            "#EXTINF:5.0,",
            "#EXT-X-MAP:URI=\"init.mp4\"\n#EXT-X-BYTERANGE:10@0\n#EXT-X-MAP:URI=\"i2.mp4\"\n#EXTINF:5.0,",
        )

        assertEquals("unknown_tag:EXT-X-KEY", HlsMediaPlaylist.parse(withKey, base).unsupportedReason)
        assertEquals("unknown_tag:EXT-X-MAP,EXT-X-BYTERANGE", HlsMediaPlaylist.parse(withMap, base).unsupportedReason)
        assertTrue(HlsMediaPlaylist.parse(withKey, base).segmentUrls.isEmpty())
    }

    @Test
    fun `끝이 없거나 VOD 가 아니거나 마스터거나 조각이 없으면 받지 않는다`() {
        assertEquals("no_endlist", HlsMediaPlaylist.parse(youtube.replace("#EXT-X-ENDLIST", ""), base).unsupportedReason)
        assertEquals("playlist_type:EVENT", HlsMediaPlaylist.parse(youtube.replace("VOD", "EVENT"), base).unsupportedReason)
        assertEquals(
            "master_playlist",
            HlsMediaPlaylist.parse("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nv.m3u8\n", base).unsupportedReason,
        )
        assertEquals("no_segments", HlsMediaPlaylist.parse("#EXTM3U\n#EXT-X-ENDLIST\n", base).unsupportedReason)
    }

    @Test
    fun `EXT 로 시작하지 않는 줄은 주석이라 건너뛴다`() {
        val withComment = youtube.replace("#EXT-X-VERSION:3", "# comment\n#EXT-X-VERSION:3")

        assertNull(HlsMediaPlaylist.parse(withComment, base).unsupportedReason)
    }

    @Test
    fun `상대 주소는 재생목록 주소를 기준으로 푼다`() {
        assertEquals(
            "https://rr1.googlevideo.com/api/manifest/hls_playlist/itag/234/seg/1.ts",
            HlsMediaPlaylist.resolve(base, "seg/1.ts"),
        )
        assertEquals("https://rr1.googlevideo.com/abs/1.ts", HlsMediaPlaylist.resolve(base, "/abs/1.ts"))
        assertEquals("https://other.example/1.ts", HlsMediaPlaylist.resolve(base, "https://other.example/1.ts"))
    }
}
