package com.kintmin.data.network.data_source

import com.kintmin.data.network.model.GitHubReleaseDto

interface GitHubReleaseDataSource {
    /** 최근에 만든 릴리스부터. */
    suspend fun fetchReleases(): Result<List<GitHubReleaseDto>>
}
