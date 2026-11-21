package com.kintmin.domain.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeUrlTest {

    @Test
    fun `watch 와 shorts 경로는 다운로드 대상이다`() {
        assertTrue(YoutubeUrl.isDownloadable("https://m.youtube.com/watch?v=abc123"))
        assertTrue(YoutubeUrl.isDownloadable("https://www.youtube.com/watch?v=abc123&t=10"))
        assertTrue(YoutubeUrl.isDownloadable("https://youtube.com/shorts/abc123"))
        assertTrue(YoutubeUrl.isDownloadable("http://m.youtube.com/shorts/abc123"))
    }

    @Test
    fun `yt-dlp 가 받는 라이브와 유튜브 뮤직 주소도 다운로드 대상이다`() {
        assertTrue(YoutubeUrl.isDownloadable("https://m.youtube.com/live/abc123"))
        assertTrue(YoutubeUrl.isDownloadable("https://www.youtube.com/live/abc123?si=x"))
        assertTrue(YoutubeUrl.isDownloadable("https://music.youtube.com/watch?v=abc123"))
        assertTrue(YoutubeUrl.isDownloadable("https://youtu.be/abc123?si=x"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/live"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/watch"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/shorts/"))
    }

    @Test
    fun `호스트 대소문자를 가리지 않는다`() {
        assertTrue(YoutubeUrl.isDownloadable("https://M.YouTube.com/watch?v=abc123"))
    }

    @Test
    fun `목록이나 홈 경로는 대상이 아니다`() {
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/feed/trending"))
        assertFalse(YoutubeUrl.isDownloadable("https://m.youtube.com/results?search_query=abc"))
    }

    @Test
    fun `다른 호스트는 대상이 아니다`() {
        assertFalse(YoutubeUrl.isDownloadable("https://vimeo.com/watch/abc123"))
        assertFalse(YoutubeUrl.isDownloadable("https://notyoutube.com/watch?v=abc123"))
    }

    @Test
    fun `scheme 이 없거나 깨진 입력은 대상이 아니다`() {
        assertFalse(YoutubeUrl.isDownloadable(""))
        assertFalse(YoutubeUrl.isDownloadable("m.youtube.com/watch?v=abc"))
        assertFalse(YoutubeUrl.isDownloadable("://"))
    }

    @Test
    fun `포트와 userinfo 가 붙어도 호스트를 바르게 읽는다`() {
        assertTrue(YoutubeUrl.isDownloadable("https://m.youtube.com:443/watch?v=abc"))
    }

    @Test
    fun `재생목록 페이지와 재생목록에서 연 영상을 가른다`() {
        assertEquals(YoutubeUrlKind.PLAYLIST, YoutubeUrl.kindOf("https://m.youtube.com/playlist?list=PLabc"))
        assertEquals(YoutubeUrlKind.PLAYLIST, YoutubeUrl.kindOf("https://music.youtube.com/playlist?list=OLAK5uy_x&si=1"))
        assertEquals(YoutubeUrlKind.VIDEO_IN_PLAYLIST, YoutubeUrl.kindOf("https://www.youtube.com/watch?v=abc&list=PLabc&index=3"))
        assertEquals(YoutubeUrlKind.VIDEO, YoutubeUrl.kindOf("https://youtu.be/abc?si=x"))
        assertEquals(YoutubeUrlKind.NOT_DOWNLOADABLE, YoutubeUrl.kindOf("https://m.youtube.com/"))
    }

    @Test
    fun `믹스는 재생목록으로 보지 않아 그 영상 하나만 받는다`() {
        val mix = "https://m.youtube.com/watch?v=KXOrq_0-cuA&list=RDKXOrq_0-cuA&start_radio=1"

        assertNull(YoutubeUrl.playlistIdOf(mix))
        assertEquals(YoutubeUrlKind.VIDEO, YoutubeUrl.kindOf(mix))
        assertEquals(YoutubeUrlKind.NOT_DOWNLOADABLE, YoutubeUrl.kindOf("https://m.youtube.com/playlist?list=RDabc"))
    }

    @Test
    fun `유튜브가 아니거나 list 가 비었으면 재생목록이 아니다`() {
        assertNull(YoutubeUrl.playlistIdOf("https://example.com/playlist?list=PLabc"))
        assertNull(YoutubeUrl.playlistIdOf("https://m.youtube.com/playlist?list="))
        assertEquals("PLabc", YoutubeUrl.playlistIdOf("https://m.youtube.com/playlist?list=PLabc#top"))
    }
}
