package com.kintmin.presentation

import com.kintmin.presentation.deeplink.DeepLinkConstants
import com.kintmin.presentation.deeplink.DeepLinkParser
import com.kintmin.presentation.navigation.NavigationIntent
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkParserTest {

    private fun parse(url: String, playlistId: Int? = null, mediaId: Int? = null) =
        DeepLinkParser.parse(url, playlistId, mediaId)

    @Test
    fun `우리 주소가 아니면 아무 명령도 내지 않는다`() {
        assertEquals(emptyList<NavigationIntent>(), parse("http://www.jellytube.com/main/step"))
        assertEquals(emptyList<NavigationIntent>(), parse("https://evil.com/main/step"))
        assertEquals(emptyList<NavigationIntent>(), parse("https://www.jellytube.com/other/step"))
        assertEquals(emptyList<NavigationIntent>(), parse("https://www.jellytube.com"))
        assertEquals(emptyList<NavigationIntent>(), parse("not a url"))
    }

    @Test
    fun `iOS 커스텀 스킴도 같은 문법으로 받는다`() {
        // 스킴만 다르고 호스트·경로는 https 쪽과 같다. 두 플랫폼이 같은 답을 내야 한다.
        assertEquals(
            parse(DeepLinkConstants.UrlBuilder.stepScreen()),
            parse("jellytube://www.jellytube.com/main/step"),
        )
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToLyricsViewer(9),
            ),
            parse("jellytube://www.jellytube.com/main/lyrics/9"),
        )
    }

    @Test
    fun `커스텀 스킴이어도 호스트가 다르면 거른다`() {
        assertEquals(emptyList<NavigationIntent>(), parse("jellytube://evil.com/main/step"))
        assertEquals(emptyList<NavigationIntent>(), parse("jellytubex://www.jellytube.com/main/step"))
    }

    @Test
    fun `신뢰 호스트를 가장한 주소를 거른다`() {
        assertEquals(
            emptyList<NavigationIntent>(),
            parse("https://www.jellytube.com@evil.com/main/step"),
        )
        assertEquals(
            emptyList<NavigationIntent>(),
            parse("https://evil.com\\www.jellytube.com/main/step"),
        )
    }

    @Test
    fun `설정 딥링크는 목록 탭을 거쳐 설정으로 간다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToSettings,
            ),
            parse(DeepLinkConstants.UrlBuilder.settingsScreen()),
        )
    }

    @Test
    fun `앱 로그 딥링크는 설정 뒤에 로그 화면을 쌓는다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToSettings,
                NavigationIntent.NavigateToSettingAppLog,
            ),
            parse(DeepLinkConstants.UrlBuilder.appLogScreen()),
        )
    }

    @Test
    fun `걸음수 딥링크는 설정 뒤에 걸음수 화면을 쌓는다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToSettings,
                NavigationIntent.NavigateToStep,
            ),
            parse(DeepLinkConstants.UrlBuilder.stepScreen()),
        )
    }

    @Test
    fun `다운로드 딥링크는 빌더가 만든 주소를 그대로 되돌린다`() {
        val target = "https://youtu.be/abc?v=1&t=2"
        val url = DeepLinkConstants.UrlBuilder.mainScreenDownloadTab(target)

        assertEquals(
            NavigationIntent.NavigateToMainDownloadTab(target),
            parse(url).last(),
        )
    }

    @Test
    fun `다운로드 딥링크는 두 번 인코딩된 주소도 받는다`() {
        // 쿼리 값을 꺼낼 때 한 번, 키 이름대로 한 번 더 — 안드로이드가
        // `Uri.getQueryParameter` + `URLDecoder.decode` 로 하던 횟수와 같다.
        val url = "https://www.jellytube.com/main/download" +
            "?encodedUrl=https%253A%252F%252Fyoutu.be%252Fabc"

        assertEquals(
            NavigationIntent.NavigateToMainDownloadTab("https://youtu.be/abc"),
            parse(url).last(),
        )
    }

    @Test
    fun `다운로드 딥링크에 주소가 없으면 null 로 흘린다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainDownloadTab(null),
            ),
            parse("https://www.jellytube.com/main/download"),
        )
    }

    @Test
    fun `플레이어 딥링크는 기본적으로 플레이어만 띄운다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToPlayer,
            ),
            parse(DeepLinkConstants.UrlBuilder.playerScreen()),
        )
    }

    @Test
    fun `재생 목록에서 온 플레이어 딥링크는 목록을 먼저 쌓는다`() {
        val url = DeepLinkConstants.UrlBuilder
            .playerScreen(DeepLinkConstants.PlayerScreenEntry.Playlist)

        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToPlaylistContent(playlistId = 7, focusAudioMediaId = 42),
                NavigationIntent.NavigateToPlayer,
            ),
            parse(url, playlistId = 7, mediaId = 42),
        )
    }

    @Test
    fun `재생 중이 아니면 재생 목록 진입이어도 플레이어만 띄운다`() {
        val url = DeepLinkConstants.UrlBuilder
            .playerScreen(DeepLinkConstants.PlayerScreenEntry.Playlist)

        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToPlayer,
            ),
            parse(url, playlistId = null, mediaId = 42),
        )
    }

    @Test
    fun `플레이리스트 딥링크는 포커스 음원까지 넘긴다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToPlaylistContent(playlistId = 3, focusAudioMediaId = 9),
            ),
            parse(DeepLinkConstants.UrlBuilder.playlistContentScreen(3, 9)),
        )
    }

    @Test
    fun `플레이리스트 안의 음원 딥링크는 상세까지 쌓는다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToPlaylistContent(playlistId = 3, focusAudioMediaId = null),
                NavigationIntent.NavigateToAudioMedia(9),
            ),
            parse(DeepLinkConstants.UrlBuilder.playlistAudioMediaDetailScreen(3, 9)),
        )
    }

    @Test
    fun `플레이리스트 아이디가 깨지면 목록 탭까지만 간다`() {
        // 뿌리 경로가 맞은 뒤의 포기라 PopAll 과 목록 탭은 이미 확정이다.
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
            ),
            parse("https://www.jellytube.com/main/playlists/abc/audioMedias"),
        )
    }

    @Test
    fun `음원 딥링크는 아이디가 깨지면 스택만 비운다`() {
        // 이 분기는 목록 탭을 쌓기 전에 포기한다 — 안드로이드 구현과 같은 순서다.
        assertEquals(
            listOf(NavigationIntent.PopAll),
            parse("https://www.jellytube.com/main/audioMedias/abc"),
        )
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToAudioMedia(9),
            ),
            parse(DeepLinkConstants.UrlBuilder.audioMediaDetailScreen(9)),
        )
    }

    @Test
    fun `가사 딥링크는 가사 뷰어로 간다`() {
        assertEquals(
            listOf(
                NavigationIntent.PopAll,
                NavigationIntent.NavigateToMainPlaylistsTab,
                NavigationIntent.NavigateToLyricsViewer(9),
            ),
            parse(DeepLinkConstants.UrlBuilder.lyricsViewerScreen(9)),
        )
        assertEquals(
            listOf(NavigationIntent.PopAll),
            parse("https://www.jellytube.com/main/lyrics/abc"),
        )
    }

    @Test
    fun `모르는 경로는 스택만 비운다`() {
        assertEquals(
            listOf(NavigationIntent.PopAll),
            parse("https://www.jellytube.com/main/unknown"),
        )
    }
}
