package com.kintmin.desktop.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DesktopRelease(
    val versionName: String,
    val downloadUrl: String,
)

@Serializable
internal data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

/** [installerExtension] 의 설치 파일이 없으면 릴리스 페이지를 준다. */
internal fun List<GitHubReleaseDto>.latestDesktopRelease(installerExtension: String?): DesktopRelease? =
    filter { !it.draft && !it.prerelease && it.tagName.startsWith(DESKTOP_TAG_PREFIX) }
        .map { release ->
            DesktopRelease(
                versionName = release.tagName.removePrefix(DESKTOP_TAG_PREFIX),
                downloadUrl = release.assets
                    .firstOrNull { installerExtension != null && it.name.endsWith(".$installerExtension") }
                    ?.browserDownloadUrl
                    ?: release.htmlUrl,
            )
        }
        .maxWithOrNull { a, b ->
            when {
                isNewerVersion(a.versionName, b.versionName) -> 1
                isNewerVersion(b.versionName, a.versionName) -> -1
                else -> 0
            }
        }

// 릴리스 CI 가 붙이는 태그 모양이다.
private const val DESKTOP_TAG_PREFIX = "desktop/v"
