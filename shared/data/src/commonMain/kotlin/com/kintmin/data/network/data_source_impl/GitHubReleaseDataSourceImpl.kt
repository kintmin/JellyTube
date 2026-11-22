package com.kintmin.data.network.data_source_impl

import com.kintmin.data.network.data_source.GitHubReleaseDataSource
import com.kintmin.data.network.model.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

internal class GitHubReleaseDataSourceImpl(
    private val client: HttpClient,
) : GitHubReleaseDataSource {

    override suspend fun fetchReleases(): Result<List<GitHubReleaseDto>> = runCatching {
        client.get(RELEASES_URL) {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            // mobile 과 desktop 릴리스가 섞여 있어, 최신 mobile 릴리스가 첫 쪽에 들어올 만큼 받는다.
            parameter("per_page", 30)
        }.body()
    }
}

private const val RELEASES_URL = "https://api.github.com/repos/kintmin/JellyTube/releases"
