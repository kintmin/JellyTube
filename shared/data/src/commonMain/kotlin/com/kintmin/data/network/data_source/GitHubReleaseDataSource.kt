package com.kintmin.data.network.data_source

import com.kintmin.data.network.model.GitHubReleaseDto

interface GitHubReleaseDataSource {
    suspend fun fetchReleases(): Result<List<GitHubReleaseDto>>
}
