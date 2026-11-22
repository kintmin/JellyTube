package com.kintmin.desktop.update

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

/** 설치된 것보다 새 릴리스가 있고 무시한 버전이 아니면 그것을 준다. 개발 실행처럼 버전을 모르면 확인하지 않는다. */
internal suspend fun findDesktopUpdate(
    installedVersion: String?,
    ignoredVersion: String?,
    fetchLatest: suspend () -> DesktopRelease?,
): DesktopRelease? {
    if (installedVersion == null) return null
    val latest = fetchLatest() ?: return null
    if (!isNewerVersion(latest.versionName, installedVersion)) return null
    if (latest.versionName == ignoredVersion) return null
    return latest
}

object DesktopUpdate {

    suspend fun find(): DesktopRelease? = findDesktopUpdate(
        installedVersion = System.getProperty(VERSION_PROPERTY),
        ignoredVersion = preferences.get(IGNORED_VERSION_KEY, null),
        fetchLatest = ::fetchLatest,
    )

    fun ignore(versionName: String) {
        preferences.put(IGNORED_VERSION_KEY, versionName)
    }

    private suspend fun fetchLatest(): DesktopRelease? = runCatching {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }.use { client ->
            client.get(RELEASES_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                // mobile 과 desktop 릴리스가 섞여 있어, 최신 desktop 릴리스가 첫 쪽에 들어올 만큼 받는다.
                parameter("per_page", 30)
            }.body<List<GitHubReleaseDto>>().latestDesktopRelease(installerExtension())
        }
    }.getOrNull()

    private fun installerExtension(): String? {
        val os = System.getProperty("os.name").orEmpty()
        return when {
            os.startsWith("Windows") -> "msi"
            os.startsWith("Mac") -> "dmg"
            else -> null
        }
    }

    private val preferences: Preferences = Preferences.userRoot().node("com/kintmin/jellytube/desktop")
}

// 설치본을 만든 jpackage 가 패키지 버전을 넣는다. 개발 실행(:desktop:run)에는 없다.
private const val VERSION_PROPERTY = "jpackage.app-version"
private const val IGNORED_VERSION_KEY = "ignoredUpdateVersion"
private const val RELEASES_URL = "https://api.github.com/repos/kintmin/JellyTube/releases"
