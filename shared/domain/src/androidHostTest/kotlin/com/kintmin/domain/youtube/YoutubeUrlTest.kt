package com.kintmin.domain.youtube

import org.junit.Assert.assertFalse
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
}
