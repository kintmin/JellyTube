package com.kintmin.desktop.update

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopReleaseTest {

    private fun releases(json: String) = Json { ignoreUnknownKeys = true }.decodeFromString<List<GitHubReleaseDto>>(json)

    private val json = """
        [
          {"tag_name": "mobile/v2.1.0", "html_url": "https://github.com/r/mobile",
           "assets": [{"name": "JellyTube_2.1.0(31)_release.apk", "browser_download_url": "https://dl/apk"}]},
          {"tag_name": "desktop/v1.0.2", "html_url": "https://github.com/r/hotfix", "assets": []},
          {"tag_name": "desktop/v1.1.0", "html_url": "https://github.com/r/desktop", "id": 1,
           "assets": [
             {"name": "JellyTube_FileShare_1.1.0.msi", "browser_download_url": "https://dl/msi"},
             {"name": "JellyTube_FileShare_1.1.0.exe", "browser_download_url": "https://dl/exe"},
             {"name": "JellyTube_FileShare_1.1.0.dmg", "browser_download_url": "https://dl/dmg"}
           ]}
        ]
    """

    @Test
    fun `mobile 릴리스를 건너뛰고 버전이 가장 높은 desktop 릴리스를 고른다`() {
        assertEquals("1.1.0", releases(json).latestDesktopRelease("msi")?.versionName)
    }

    @Test
    fun `OS 에 맞는 설치 파일 주소를 준다`() {
        assertEquals("https://dl/msi", releases(json).latestDesktopRelease("msi")?.downloadUrl)
        assertEquals("https://dl/dmg", releases(json).latestDesktopRelease("dmg")?.downloadUrl)
    }

    @Test
    fun `맞는 설치 파일이 없으면 릴리스 페이지를 준다`() {
        assertEquals("https://github.com/r/desktop", releases(json).latestDesktopRelease(null)?.downloadUrl)
    }

    @Test
    fun `프리릴리스와 초안은 고르지 않는다`() {
        val json = """
            [
              {"tag_name": "desktop/v1.2.0", "html_url": "x", "prerelease": true},
              {"tag_name": "desktop/v1.3.0", "html_url": "x", "draft": true}
            ]
        """
        assertNull(releases(json).latestDesktopRelease("msi"))
    }

    @Test
    fun `버전은 자리마다 숫자로 비교하고 접미사는 무시한다`() {
        assertEquals(true, isNewerVersion("1.10.0", "1.9.9"))
        assertEquals(false, isNewerVersion("1.0", "1.0.0"))
        assertEquals(false, isNewerVersion("1.0.1", "1.0.1-dev"))
    }
}
