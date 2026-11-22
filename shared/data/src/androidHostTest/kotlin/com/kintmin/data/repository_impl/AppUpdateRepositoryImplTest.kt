package com.kintmin.data.repository_impl

import com.kintmin.data.network.data_source.GitHubReleaseDataSource
import com.kintmin.data.network.model.GitHubReleaseDto
import com.kintmin.domain.app_update.model.AppRelease
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateRepositoryImplTest {

    private fun repository(releasesJson: String, installerExtension: String = "apk") = AppUpdateRepositoryImpl(
        gitHubReleaseDataSource = mockk<GitHubReleaseDataSource> {
            coEvery { fetchReleases() } returns Result.success(
                Json { ignoreUnknownKeys = true }.decodeFromString<List<GitHubReleaseDto>>(releasesJson),
            )
        },
        datastoreUtil = mockk(relaxed = true),
        installerExtension = installerExtension,
    )

    @Test
    fun `desktop 릴리스를 건너뛰고 최신 mobile 릴리스의 APK 주소를 준다`() = runTest {
        val json = """
            [
              {"tag_name": "desktop/v1.1.0", "html_url": "https://github.com/r/desktop", "draft": false,
               "prerelease": false, "assets": [{"name": "JellyTube.msi", "browser_download_url": "https://dl/msi"}]},
              {"tag_name": "mobile/v2.1.0", "html_url": "https://github.com/r/mobile", "draft": false,
               "prerelease": false, "id": 1, "body": "notes",
               "assets": [
                 {"name": "JellyTube.ipa", "browser_download_url": "https://dl/ipa"},
                 {"name": "JellyTube_2.1.0(31)_release.apk", "browser_download_url": "https://dl/apk"},
                 {"name": "source.json", "browser_download_url": "https://dl/source"}
               ]},
              {"tag_name": "mobile/v2.0.0", "html_url": "https://github.com/r/old", "assets": []}
            ]
        """

        val latest = repository(json).fetchLatestRelease().getOrThrow()

        assertEquals(AppRelease(versionName = "2.1.0", downloadUrl = "https://dl/apk"), latest)
    }

    @Test
    fun `iOS 는 같은 릴리스의 IPA 주소를 받는다`() = runTest {
        val json = """
            [{"tag_name": "mobile/v2.1.0", "html_url": "https://github.com/r/mobile", "assets": [
              {"name": "JellyTube_2.1.0(31)_release.apk", "browser_download_url": "https://dl/apk"},
              {"name": "JellyTube_2.1.0(31)_release.ipa", "browser_download_url": "https://dl/ipa"}
            ]}]
        """

        assertEquals("https://dl/ipa", repository(json, installerExtension = "ipa").fetchLatestRelease().getOrThrow()?.downloadUrl)
    }

    @Test
    fun `APK 가 없으면 릴리스 페이지를 준다`() = runTest {
        val json = """[{"tag_name": "mobile/v2.1.0", "html_url": "https://github.com/r/mobile", "assets": []}]"""

        val latest = repository(json).fetchLatestRelease().getOrThrow()

        assertEquals("https://github.com/r/mobile", latest?.downloadUrl)
    }

    @Test
    fun `나중에 낸 이전 라인 핫픽스보다 버전이 높은 릴리스를 고른다`() = runTest {
        val json = """
            [
              {"tag_name": "mobile/v1.9.1", "html_url": "https://github.com/r/hotfix"},
              {"tag_name": "mobile/v2.0.0", "html_url": "https://github.com/r/new"}
            ]
        """

        assertEquals("2.0.0", repository(json).fetchLatestRelease().getOrThrow()?.versionName)
    }

    @Test
    fun `프리릴리스는 고르지 않는다`() = runTest {
        val json = """[{"tag_name": "mobile/v2.1.0", "html_url": "https://github.com/r/mobile", "prerelease": true}]"""

        assertNull(repository(json).fetchLatestRelease().getOrThrow())
    }
}
