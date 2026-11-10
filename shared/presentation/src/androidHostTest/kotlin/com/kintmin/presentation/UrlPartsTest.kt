package com.kintmin.presentation

import com.kintmin.presentation.deeplink.UrlParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlPartsTest {

    @Test
    fun `scheme 은 점을 포함한 커스텀 스킴도 통째로 돌려준다`() {
        assertEquals("https", UrlParts.scheme("https://www.jellytube.com/main"))
        assertEquals("jellytube.dev", UrlParts.scheme("jellytube.dev://main/player"))
        assertEquals("a+b-c.d", UrlParts.scheme("a+b-c.d://host"))
    }

    @Test
    fun `scheme 은 스킴이 아닌 것을 스킴으로 읽지 않는다`() {
        assertNull(UrlParts.scheme("www.jellytube.com/main"))
        assertNull(UrlParts.scheme("://host"))
        assertNull(UrlParts.scheme("1https://host"))
        assertNull(UrlParts.scheme("ht tp://host"))
        assertNull(UrlParts.scheme("/main/player"))
    }

    @Test
    fun `scheme 을 소문자로 바꾸지 않는다`() {
        assertEquals("HTTPS", UrlParts.scheme("HTTPS://www.jellytube.com"))
    }

    @Test
    fun `host 는 포트와 userinfo 를 떼어낸다`() {
        assertEquals("www.jellytube.com", UrlParts.host("https://www.jellytube.com/main"))
        assertEquals("www.jellytube.com", UrlParts.host("https://www.jellytube.com:8443/main"))
        assertEquals("www.jellytube.com", UrlParts.host("https://user:pw@www.jellytube.com/main"))
    }

    @Test
    fun `host 는 userinfo 를 마지막 골뱅이까지로 본다`() {
        // 첫 골뱅이로 자르면 신뢰 호스트를 가장한 주소가 통과한다.
        assertEquals("evil.com", UrlParts.host("https://www.jellytube.com@evil.com/main"))
        assertEquals("evil.com", UrlParts.host("https://a@www.jellytube.com@evil.com/main"))
    }

    @Test
    fun `host 는 역슬래시도 authority 의 끝으로 본다`() {
        // 브라우저(WHATWG)와 같게 읽지 않으면 앱이 보는 호스트와 실제로 열리는 호스트가 갈린다.
        assertEquals("evil.com", UrlParts.host("https://evil.com\\www.jellytube.com/main"))
    }

    @Test
    fun `host 는 정규화하지 않는다`() {
        // 퍼센트 인코딩된 호스트를 디코딩해서 통과시키지 않는다.
        assertEquals("www%2Ejellytube%2Ecom", UrlParts.host("https://www%2Ejellytube%2Ecom/main"))
        assertNull(UrlParts.host("www.jellytube.com/main"))
    }

    @Test
    fun `host 는 IPv6 리터럴을 지원하지 않는다`() {
        // 깨진 값으로 나와 호스트 검사에서 떨어진다. 딥링크 호스트는 고정 도메인 하나라 그 실패가 맞다.
        assertEquals("[", UrlParts.host("https://[::1]:80/main"))
    }

    @Test
    fun `pathSegments 는 빈 세그먼트를 버리고 디코딩한다`() {
        assertEquals(
            listOf("main", "playlists", "3", "audioMedias"),
            UrlParts.pathSegments("https://www.jellytube.com/main/playlists/3/audioMedias"),
        )
        assertEquals(
            listOf("main", "playlists"),
            UrlParts.pathSegments("https://www.jellytube.com//main///playlists//"),
        )
        assertEquals(
            listOf("main", "한글 경로"),
            UrlParts.pathSegments("https://www.jellytube.com/main/%ED%95%9C%EA%B8%80%20%EA%B2%BD%EB%A1%9C"),
        )
    }

    @Test
    fun `pathSegments 는 프래그먼트 안의 경로를 경로로 읽지 않는다`() {
        // 경로가 없는 주소의 프래그먼트에 슬래시가 있으면 authority 스캔이 그 너머까지 간다.
        assertEquals(emptyList<String>(), UrlParts.pathSegments("https://www.jellytube.com#/main/download"))
        assertEquals(emptyList<String>(), UrlParts.pathSegments("https://www.jellytube.com?next=/main/download"))
    }

    @Test
    fun `queryParameter 는 프래그먼트 안의 쿼리를 읽지 않는다`() {
        assertNull(UrlParts.queryParameter("https://www.jellytube.com#/main?encodedUrl=https%3A%2F%2Fevil.example", "encodedUrl"))
    }

    @Test
    fun `pathSegments 는 쿼리와 프래그먼트를 제외한다`() {
        assertEquals(
            listOf("main", "download"),
            UrlParts.pathSegments("https://www.jellytube.com/main/download?encodedUrl=x#frag"),
        )
        assertEquals(emptyList<String>(), UrlParts.pathSegments("https://www.jellytube.com"))
        assertEquals(emptyList<String>(), UrlParts.pathSegments("/main/player"))
    }

    @Test
    fun `pathSegments 는 더하기를 공백으로 바꾸지 않는다`() {
        assertEquals(listOf("a+b"), UrlParts.pathSegments("https://host/a+b"))
    }

    @Test
    fun `queryParameter 는 값을 디코딩하고 더하기를 공백으로 바꾼다`() {
        assertEquals(
            "https://youtu.be/abc",
            UrlParts.queryParameter(
                "https://www.jellytube.com/main/download?encodedUrl=https%3A%2F%2Fyoutu.be%2Fabc",
                "encodedUrl",
            ),
        )
        assertEquals("a b", UrlParts.queryParameter("https://host/p?q=a+b", "q"))
        assertEquals("", UrlParts.queryParameter("https://host/p?q", "q"))
        assertEquals("", UrlParts.queryParameter("https://host/p?q=", "q"))
    }

    @Test
    fun `queryParameter 는 같은 키가 여럿이면 첫 번째를 쓴다`() {
        // 뒤가 앞을 덮으면 검증 통과용 값을 앞에 두고 진짜 값을 뒤에 붙이는 수법이 성립한다.
        assertEquals("first", UrlParts.queryParameter("https://host/p?q=first&q=second", "q"))
    }

    @Test
    fun `queryParameter 는 없는 키와 없는 쿼리에 null 을 준다`() {
        assertNull(UrlParts.queryParameter("https://host/p?a=1", "b"))
        assertNull(UrlParts.queryParameter("https://host/p", "a"))
        assertNull(UrlParts.queryParameter("https://host/p?a=1#q=2", "q"))
    }

    @Test
    fun `percentDecode 는 여러 바이트 글자를 한 번에 해석한다`() {
        assertEquals("한글", UrlParts.percentDecode("%ED%95%9C%EA%B8%80"))
        assertEquals("a한b", UrlParts.percentDecode("a%ED%95%9Cb"))
    }

    @Test
    fun `percentDecode 는 깨진 퍼센트를 그대로 남긴다`() {
        assertEquals("%", UrlParts.percentDecode("%"))
        assertEquals("%4", UrlParts.percentDecode("%4"))
        assertEquals("%zz", UrlParts.percentDecode("%zz"))
        assertEquals("A%zz", UrlParts.percentDecode("%41%zz"))
    }
}
